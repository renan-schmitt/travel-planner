package com.travelplanner.suggestion.domain;

import com.travelplanner.auth.domain.AppUserService;
import com.travelplanner.shared.web.InvalidRequestException;
import com.travelplanner.shared.web.NotFoundException;
import com.travelplanner.suggestion.infra.SuggestionRepository;
import com.travelplanner.trip.domain.TripAccessService;
import com.travelplanner.trip.domain.TripRole;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/** Sugestões da viagem: leitura ({@code VIEWER}) e decisão do usuário ({@code EDITOR}). */
@Service
@RequiredArgsConstructor
public class SuggestionService {

  private static final Comparator<SuggestionView> ORDER =
      Comparator.comparing((SuggestionView v) -> v.suggestion().getSeverity())
          .reversed()
          .thenComparing(v -> v.suggestion().getRuleCode())
          .thenComparing(v -> v.suggestion().getId());

  private final SuggestionRepository suggestions;
  private final TripAccessService access;
  private final AppUserService users;
  private final SuggestionEngine engine;
  private final JsonMapper json;

  /** Sugestão com o estado efetivo de hoje e o payload já lido. */
  public record SuggestionView(
      Suggestion suggestion, SuggestionStatus status, SuggestionPayload payload) {}

  /** Mais graves primeiro; {@code status} nulo = todas. */
  @Transactional(readOnly = true)
  public List<SuggestionView> list(long tripId, long userId, SuggestionStatus status) {
    access.requireRole(tripId, userId, TripRole.VIEWER);
    LocalDate today = today(userId);
    return suggestions.findByTripId(tripId).stream()
        .map(s -> view(s, today))
        .filter(v -> status == null || v.status() == status)
        .sorted(ORDER)
        .toList();
  }

  /**
   * Pendentes por viagem e severidade, numa só consulta (viagens sem pendências ficam fora). Sem
   * verificação de acesso: quem chama já sabe que o usuário é membro das viagens.
   */
  @Transactional(readOnly = true)
  public Map<Long, Map<Severity, Integer>> countPendingBySeverity(
      Collection<Long> tripIds, LocalDate today) {
    Map<Long, Map<Severity, Integer>> result = new HashMap<>();
    if (tripIds.isEmpty()) {
      return result;
    }
    for (Object[] row : suggestions.countPendingBySeverity(tripIds, today)) {
      result
          .computeIfAbsent((Long) row[0], id -> new EnumMap<>(Severity.class))
          .put((Severity) row[1], ((Number) row[2]).intValue());
    }
    return result;
  }

  /** Pendentes (efetivas hoje) de uma severidade nas viagens. Sem verificação de acesso. */
  @Transactional(readOnly = true)
  public List<SuggestionView> pending(
      Collection<Long> tripIds, Severity severity, LocalDate today) {
    if (tripIds.isEmpty()) {
      return List.of();
    }
    return suggestions.findPendingBySeverity(tripIds, severity, today).stream()
        .map(s -> view(s, today))
        .toList();
  }

  /**
   * Aceita, adia (até {@code snoozedUntil}, depois de hoje), dispensa ou reabre. Idempotente.
   * Aceitar uma sugestão com item de checklist cria o item (se ainda não existe).
   */
  @Transactional
  public SuggestionView decide(
      long tripId,
      long suggestionId,
      long userId,
      SuggestionStatus status,
      LocalDate snoozedUntil) {
    access.requireRole(tripId, userId, TripRole.EDITOR);
    Suggestion suggestion =
        suggestions
            .findByIdAndTripId(suggestionId, tripId)
            .orElseThrow(() -> new NotFoundException("Sugestão não encontrada."));
    LocalDate today = today(userId);
    switch (status) {
      case ACCEPTED -> {
        suggestion.accept();
        ChecklistDraft checklist = payload(suggestion).checklist();
        if (checklist != null) {
          engine.ensureChecklist(
              tripId, suggestion.getRuleCode(), suggestion.getLogicalKey(), checklist);
        }
      }
      case DISMISSED -> suggestion.dismiss();
      case PENDING -> suggestion.reopen();
      case SNOOZED -> {
        if (snoozedUntil == null || !snoozedUntil.isAfter(today)) {
          throw new InvalidRequestException("snoozedUntil", "Escolha uma data depois de hoje.");
        }
        suggestion.snooze(snoozedUntil);
      }
    }
    return view(suggestions.saveAndFlush(suggestion), today);
  }

  private SuggestionView view(Suggestion suggestion, LocalDate today) {
    return new SuggestionView(suggestion, suggestion.effectiveStatus(today), payload(suggestion));
  }

  private SuggestionPayload payload(Suggestion suggestion) {
    return json.readValue(suggestion.getPayloadJson(), SuggestionPayload.class);
  }

  private LocalDate today(long userId) {
    return engine.today(users.get(userId).getHomeCity());
  }
}
