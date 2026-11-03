package com.travelplanner.checklist.domain;

import com.travelplanner.checklist.infra.ChecklistItemRepository;
import com.travelplanner.shared.web.InvalidRequestException;
import com.travelplanner.shared.web.NotFoundException;
import com.travelplanner.trip.domain.TripAccessService;
import com.travelplanner.trip.domain.TripMemberRemovedEvent;
import com.travelplanner.trip.domain.TripRole;
import java.time.Clock;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Itens de checklist da viagem. Leitura exige {@code VIEWER}; alterações, {@code EDITOR}. Itens
 * novos entram no fim da lista; a ordem é a do usuário ({@link #reorder}).
 */
@Service
@RequiredArgsConstructor
public class ChecklistService {

  private final ChecklistItemRepository items;
  private final TripAccessService access;
  private final Clock clock;

  @Transactional(readOnly = true)
  public List<ChecklistItem> list(long tripId, long userId, ChecklistFilter filter) {
    access.requireRole(tripId, userId, TripRole.VIEWER);
    return items.findByTripIdOrderBySortOrderAscIdAsc(tripId).stream()
        .filter(i -> filter.matches(i, userId))
        .toList();
  }

  @Transactional
  public ChecklistItem create(long tripId, long userId, ChecklistItemDetails details) {
    access.requireRole(tripId, userId, TripRole.EDITOR);
    checkAssignee(tripId, details);
    return items.saveAndFlush(
        new ChecklistItem(tripId, ChecklistSource.MANUAL, details, items.maxSortOrder(tripId) + 1));
  }

  @Transactional
  public ChecklistItem update(
      long tripId, long itemId, long userId, long version, ChecklistItemDetails details) {
    access.requireRole(tripId, userId, TripRole.EDITOR);
    ChecklistItem item = find(tripId, itemId);
    if (item.getVersion() != version) {
      throw new OptimisticLockingFailureException("Versão do item divergente.");
    }
    // Manter um responsável que já saiu da viagem não é permitido; o evento de saída já o tira.
    checkAssignee(tripId, details);
    item.update(details);
    return items.saveAndFlush(item);
  }

  /**
   * Marca como concluído ou aberto, sem {@code version}: repetir o pedido dá o mesmo resultado
   * (importante para a fila offline, Fase 28).
   */
  @Transactional
  public ChecklistItem changeStatus(long tripId, long itemId, long userId, ChecklistStatus status) {
    access.requireRole(tripId, userId, TripRole.EDITOR);
    ChecklistItem item = find(tripId, itemId);
    if (item.changeStatus(status, userId, clock.instant())) {
      items.saveAndFlush(item);
    }
    return item;
  }

  @Transactional
  public void delete(long tripId, long itemId, long userId) {
    access.requireRole(tripId, userId, TripRole.EDITOR);
    items.delete(find(tripId, itemId));
    items.flush();
  }

  /**
   * Nova ordem: todos os ids dos itens da viagem, sem repetição.
   *
   * @throws InvalidRequestException se faltar ou sobrar algum id
   */
  @Transactional
  public List<ChecklistItem> reorder(long tripId, long userId, List<Long> itemIds) {
    access.requireRole(tripId, userId, TripRole.EDITOR);
    List<ChecklistItem> current = items.findByTripIdOrderBySortOrderAscIdAsc(tripId);
    Map<Long, ChecklistItem> byId =
        current.stream().collect(Collectors.toMap(ChecklistItem::getId, Function.identity()));
    if (itemIds.size() != current.size()
        || new HashSet<>(itemIds).size() != itemIds.size()
        || !byId.keySet().containsAll(itemIds)) {
      throw new InvalidRequestException(
          "itemIds", "A lista precisa ter todos os itens da viagem, sem repetição.");
    }
    List<ChecklistItem> ordered = itemIds.stream().map(byId::get).toList();
    for (int i = 0; i < ordered.size(); i++) {
      ordered.get(i).moveTo(i);
    }
    items.saveAllAndFlush(ordered);
    return ordered;
  }

  /** Quem sai da viagem deixa de ser responsável pelos itens dela. */
  @EventListener
  @Transactional
  public void onMemberRemoved(TripMemberRemovedEvent event) {
    items.unassign(event.tripId(), event.userId(), clock.instant());
  }

  private void checkAssignee(long tripId, ChecklistItemDetails details) {
    if (details.assigneeUserId() != null && !access.isMember(tripId, details.assigneeUserId())) {
      throw new InvalidRequestException(
          "assigneeUserId", "O responsável precisa participar da viagem.");
    }
  }

  private ChecklistItem find(long tripId, long itemId) {
    return items
        .findByIdAndTripId(itemId, tripId)
        .orElseThrow(() -> new NotFoundException("Item não encontrado."));
  }
}
