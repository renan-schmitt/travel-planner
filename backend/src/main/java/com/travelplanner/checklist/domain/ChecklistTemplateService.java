package com.travelplanner.checklist.domain;

import com.travelplanner.checklist.infra.ChecklistItemRepository;
import com.travelplanner.checklist.infra.ChecklistTemplateRepository;
import com.travelplanner.shared.web.ForbiddenException;
import com.travelplanner.shared.web.NotFoundException;
import com.travelplanner.trip.domain.TripAccessService;
import com.travelplanner.trip.domain.TripRole;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Templates de checklist: os do sistema (só leitura) e os do usuário (só ele vê e edita). Aplicar
 * um template numa viagem cria itens {@code TEMPLATE} sem repetir os que já existem.
 */
@Service
@RequiredArgsConstructor
public class ChecklistTemplateService {

  private final ChecklistTemplateRepository templates;
  private final ChecklistItemRepository items;
  private final TripAccessService access;

  @Transactional(readOnly = true)
  public List<ChecklistTemplate> list(long userId) {
    return templates.findVisibleTo(userId);
  }

  @Transactional(readOnly = true)
  public ChecklistTemplate get(long templateId, long userId) {
    ChecklistTemplate template = findVisible(templateId, userId);
    template.getItems().size(); // carrega os itens dentro da transação
    return template;
  }

  @Transactional
  public ChecklistTemplate create(long userId, TemplateDetails details) {
    return templates.saveAndFlush(new ChecklistTemplate(userId, details));
  }

  @Transactional
  public ChecklistTemplate update(
      long templateId, long userId, long version, TemplateDetails details) {
    ChecklistTemplate template = findOwn(templateId, userId);
    if (template.getVersion() != version) {
      throw new OptimisticLockingFailureException("Versão do template divergente.");
    }
    template.update(details);
    return templates.saveAndFlush(template);
  }

  @Transactional
  public void delete(long templateId, long userId) {
    templates.delete(findOwn(templateId, userId));
    templates.flush();
  }

  /**
   * Cria os itens do template no fim do checklist ({@code source = TEMPLATE}). A data limite é a
   * partida + {@code dueOffsetDays} (sem partida, sem data). Itens com o mesmo título e categoria
   * de um já existente (sem diferenciar maiúsculas, acentos e espaços) são pulados.
   */
  @Transactional
  public TemplateApplyResult apply(long tripId, long templateId, long userId) {
    LocalDate start = access.requireRole(tripId, userId, TripRole.EDITOR).getTrip().getStartDate();
    ChecklistTemplate template = findVisible(templateId, userId);
    Set<String> existing = new HashSet<>();
    items.findByTripIdOrderBySortOrderAscIdAsc(tripId).forEach(i -> existing.add(key(i)));
    int sortOrder = items.maxSortOrder(tripId) + 1;
    List<ChecklistItem> added = new ArrayList<>();
    int skipped = 0;
    for (TemplateItem t : template.getItems()) {
      if (!existing.add(key(t.getCategory(), t.getTitle()))) {
        skipped++;
        continue;
      }
      LocalDate due =
          start == null || t.getDueOffsetDays() == null
              ? null
              : start.plusDays(t.getDueOffsetDays());
      added.add(
          new ChecklistItem(
              tripId,
              ChecklistSource.TEMPLATE,
              new ChecklistItemDetails(t.getCategory(), t.getTitle(), null, due, null),
              sortOrder++));
    }
    return new TemplateApplyResult(items.saveAllAndFlush(added), skipped);
  }

  /**
   * Novo template do usuário com os itens atuais da viagem, na ordem do checklist. A data limite
   * vira offset em relação à partida (sem partida ou sem data, sem offset). Itens criados pelo
   * motor de lacunas ({@code RULE}) ficam de fora: são específicos desta viagem.
   */
  @Transactional
  public ChecklistTemplate saveTripAsTemplate(long tripId, long userId, String name) {
    LocalDate start = access.requireRole(tripId, userId, TripRole.VIEWER).getTrip().getStartDate();
    Set<String> seen = new HashSet<>();
    List<TemplateItemDetails> templateItems =
        items.findByTripIdOrderBySortOrderAscIdAsc(tripId).stream()
            .filter(i -> i.getSource() != ChecklistSource.RULE)
            .filter(i -> seen.add(key(i)))
            .map(
                i ->
                    new TemplateItemDetails(
                        i.getCategory(),
                        i.getTitle(),
                        start == null || i.getDueDate() == null
                            ? null
                            : Math.toIntExact(ChronoUnit.DAYS.between(start, i.getDueDate()))))
            .toList();
    return templates.saveAndFlush(
        new ChecklistTemplate(
            userId, new TemplateDetails(name, null, TemplateIcon.LIST, templateItems)));
  }

  private ChecklistTemplate findVisible(long templateId, long userId) {
    return templates
        .findById(templateId)
        .filter(t -> t.isSystem() || t.getOwnerUserId() == userId)
        .orElseThrow(() -> new NotFoundException("Template não encontrado."));
  }

  /** Template que o usuário pode alterar: do sistema → 403; de outra pessoa → 404. */
  private ChecklistTemplate findOwn(long templateId, long userId) {
    ChecklistTemplate template = findVisible(templateId, userId);
    if (template.isSystem()) {
      throw new ForbiddenException("Templates do sistema não podem ser alterados.");
    }
    return template;
  }

  private static String key(ChecklistItem item) {
    return key(item.getCategory(), item.getTitle());
  }

  /** Categoria + título normalizado (sem acentos, minúsculo, espaços simples). */
  static String key(ChecklistCategory category, String title) {
    String plain =
        Normalizer.normalize(title, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toLowerCase(Locale.ROOT);
    return category + ":" + plain.strip().replaceAll("\\s+", " ");
  }
}
