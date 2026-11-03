package com.travelplanner.suggestion.domain;

import com.travelplanner.auth.domain.HomeCity;
import com.travelplanner.checklist.domain.ChecklistItemDetails;
import com.travelplanner.checklist.domain.ChecklistService;
import com.travelplanner.itinerary.domain.TripSnapshot;
import com.travelplanner.itinerary.domain.TripSnapshotLoader;
import com.travelplanner.shared.time.TimeProperties;
import com.travelplanner.suggestion.infra.SuggestionRepository;
import com.travelplanner.trip.infra.TripRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * Motor de lacunas: executa todas as {@link GapRule}s sobre a viagem e <b>reconcilia</b> o
 * resultado com as sugestões gravadas.
 *
 * <ul>
 *   <li>draft novo → {@code PENDING};
 *   <li>draft que já existia → atualiza severidade e parâmetros; {@code ACCEPTED}/{@code DISMISSED}
 *       mantêm o estado (salvo se a severidade subiu); {@code SNOOZED} vencida volta a {@code
 *       PENDING};
 *   <li>sugestão {@code PENDING}/{@code SNOOZED} que nenhuma regra produziu mais → apagada (a
 *       lacuna foi resolvida);
 *   <li>draft com checklist automático → cria/atualiza o item {@code RULE} (idempotente).
 * </ul>
 *
 * Uma regra que lança exceção é registrada no log e as sugestões dela ficam como estão.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SuggestionEngine {

  private final List<GapRule> rules;
  private final TripRepository trips;
  private final TripSnapshotLoader snapshots;
  private final ChecklistService checklist;
  private final SuggestionRepository suggestions;
  private final JsonMapper json;
  private final TimeProperties timeProperties;
  private final Clock clock;

  private record Key(String ruleCode, String logicalKey) {}

  /** Reconcilia as sugestões da viagem. Viagem inexistente (ex.: acabou de ser excluída): nada. */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void run(long tripId) {
    if (trips.lockById(tripId).isEmpty()) {
      return;
    }
    TripSnapshot snapshot = snapshots.load(tripId);
    GapContext context =
        new GapContext(
            snapshot,
            clock,
            checklist.itemsForRules(tripId).stream()
                .map(
                    i ->
                        new GapContext.ChecklistEntry(
                            i.getRuleCode(), i.getRuleKey(), i.getTitle(), i.getStatus()))
                .toList());
    LocalDate today = today(snapshot.owner().homeCity());

    Map<Key, Suggestion> existing = new HashMap<>();
    suggestions
        .findByTripId(tripId)
        .forEach(s -> existing.put(new Key(s.getRuleCode(), s.getLogicalKey()), s));
    Set<Key> produced = new HashSet<>();
    Set<String> failedRules = new HashSet<>();

    for (GapRule rule : rules) {
      List<SuggestionDraft> drafts;
      try {
        drafts = rule.evaluate(context);
      } catch (RuntimeException e) {
        log.error("Regra {} falhou na viagem {}", rule.code(), tripId, e);
        failedRules.add(rule.code());
        continue;
      }
      for (SuggestionDraft draft : drafts) {
        Key key = new Key(rule.code(), draft.logicalKey());
        if (!produced.add(key)) {
          continue;
        }
        apply(tripId, key, draft, existing.get(key), today);
      }
    }

    existing.forEach(
        (key, suggestion) -> {
          if (!produced.contains(key)
              && !failedRules.contains(key.ruleCode())
              && suggestion.open()) {
            suggestions.delete(suggestion);
          }
        });
    suggestions.flush();
  }

  private void apply(
      long tripId, Key key, SuggestionDraft draft, Suggestion current, LocalDate today) {
    SuggestionPayload payload =
        new SuggestionPayload(draft.params(), draft.autoChecklist().orElse(null));
    String payloadJson = json.writeValueAsString(payload);
    if (current == null) {
      suggestions.save(
          new Suggestion(tripId, key.ruleCode(), key.logicalKey(), draft.severity(), payloadJson));
    } else {
      boolean samePayload =
          json.readTree(current.getPayloadJson()).equals(json.readTree(payloadJson));
      current.refresh(draft.severity(), samePayload ? null : payloadJson, today);
    }
    draft
        .autoChecklist()
        .filter(ChecklistDraft::automatic)
        .ifPresent(c -> ensureChecklist(tripId, key.ruleCode(), key.logicalKey(), c));
  }

  /** Cria (ou acompanha o prazo do) item de checklist da sugestão. */
  void ensureChecklist(long tripId, String ruleCode, String logicalKey, ChecklistDraft draft) {
    checklist.ensureRuleItem(
        tripId,
        ruleCode,
        logicalKey,
        new ChecklistItemDetails(draft.category(), draft.title(), null, draft.dueDate(), null));
  }

  LocalDate today(HomeCity home) {
    ZoneId zone =
        home != null && home.timezone() != null
            ? ZoneId.of(home.timezone())
            : timeProperties.defaultZone();
    return LocalDate.now(clock.withZone(zone));
  }
}
