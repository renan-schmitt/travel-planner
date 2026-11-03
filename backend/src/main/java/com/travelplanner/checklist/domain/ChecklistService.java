package com.travelplanner.checklist.domain;

import com.travelplanner.checklist.infra.ChecklistItemRepository;
import com.travelplanner.shared.web.InvalidRequestException;
import com.travelplanner.shared.web.NotFoundException;
import com.travelplanner.trip.domain.TripAccessService;
import com.travelplanner.trip.domain.TripChangedEvent;
import com.travelplanner.trip.domain.TripMemberRemovedEvent;
import com.travelplanner.trip.domain.TripRole;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Itens de checklist da viagem. Leitura exige {@code VIEWER}; alterações, {@code EDITOR}. Itens
 * novos entram no fim da lista; a ordem é a do usuário ({@link #reorder}). Criar, editar e excluir
 * publicam {@link TripChangedEvent} (regras como {@code SEGURO_VIAGEM} olham o checklist); marcar
 * como feito e reordenar, não.
 */
@Service
@RequiredArgsConstructor
public class ChecklistService {

  private final ChecklistItemRepository items;
  private final TripAccessService access;
  private final Clock clock;
  private final ApplicationEventPublisher events;

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
    ChecklistItem item =
        items.saveAndFlush(
            new ChecklistItem(
                tripId, ChecklistSource.MANUAL, details, items.maxSortOrder(tripId) + 1));
    events.publishEvent(new TripChangedEvent(tripId));
    return item;
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
    ChecklistItem saved = items.saveAndFlush(item);
    events.publishEvent(new TripChangedEvent(tripId));
    return saved;
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
    events.publishEvent(new TripChangedEvent(tripId));
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

  /**
   * Itens da viagem para as regras do motor de lacunas. Sem verificação de acesso (processo
   * interno).
   */
  @Transactional(readOnly = true)
  public List<ChecklistItem> itemsForRules(long tripId) {
    return items.findByTripIdOrderBySortOrderAscIdAsc(tripId);
  }

  /** Progresso do checklist: total de itens e quantos estão feitos. */
  public record Progress(int total, int done) {}

  /**
   * Progresso por viagem, numa só consulta (viagens sem itens ficam fora do mapa). Sem verificação
   * de acesso: quem chama já sabe que o usuário é membro das viagens.
   */
  @Transactional(readOnly = true)
  public Map<Long, Progress> progress(Collection<Long> tripIds) {
    Map<Long, Progress> result = new HashMap<>();
    if (tripIds.isEmpty()) {
      return result;
    }
    for (Object[] row : items.countProgress(tripIds)) {
      result.put(
          (Long) row[0], new Progress(((Number) row[1]).intValue(), ((Number) row[2]).intValue()));
    }
    return result;
  }

  /**
   * Itens abertos das viagens com prazo até {@code until} (inclusive, com os vencidos), por prazo.
   * Sem verificação de acesso, como {@link #progress}.
   */
  @Transactional(readOnly = true)
  public List<ChecklistItem> openDueUntil(Collection<Long> tripIds, LocalDate until) {
    return tripIds.isEmpty() ? List.of() : items.findOpenDueUntil(tripIds, until);
  }

  /**
   * Garante o item de uma regra (idempotente): cria se não existe; se existe e está aberto, só
   * acompanha o prazo. Sem verificação de acesso e sem {@link TripChangedEvent} (quem chama é o
   * próprio motor).
   */
  @Transactional
  public ChecklistItem ensureRuleItem(
      long tripId, String ruleCode, String ruleKey, ChecklistItemDetails details) {
    return items
        .findByTripIdAndRuleCodeAndRuleKey(tripId, ruleCode, ruleKey)
        .map(
            item -> {
              if (item.getStatus() == ChecklistStatus.OPEN
                  && !Objects.equals(item.getDueDate(), details.dueDate())) {
                item.reschedule(details.dueDate());
              }
              return item;
            })
        .orElseGet(
            () ->
                items.save(
                    ChecklistItem.fromRule(
                        tripId, ruleCode, ruleKey, details, items.maxSortOrder(tripId) + 1)));
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
