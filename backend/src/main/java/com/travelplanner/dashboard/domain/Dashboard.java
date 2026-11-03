package com.travelplanner.dashboard.domain;

import com.travelplanner.checklist.domain.ChecklistCategory;
import com.travelplanner.suggestion.domain.Severity;
import com.travelplanner.trip.domain.TripView;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Tela inicial: próximas viagens (com progresso), alertas com prazo e as últimas viagens passadas.
 */
public record Dashboard(
    List<UpcomingTrip> upcomingTrips, List<Alert> alerts, List<TripView> pastTrips) {

  /**
   * Viagem que ainda não terminou. {@code daysUntilStart}: dias até a partida (0 = hoje, negativo =
   * já começou; {@code null} sem data de início).
   */
  public record UpcomingTrip(
      TripView view,
      Integer daysUntilStart,
      int checklistDone,
      int checklistTotal,
      Map<Severity, Integer> pendingSuggestions) {}

  /** Origem do alerta. */
  public enum AlertKind {
    CHECKLIST_ITEM,
    SUGGESTION
  }

  /**
   * Alerta: item de checklist aberto com prazo próximo ou vencido, ou sugestão {@code CRITICAL}
   * pendente. {@code date}: o prazo do item ou, na sugestão, a partida da viagem.
   */
  public record Alert(
      AlertKind kind,
      long tripId,
      String tripName,
      Severity severity,
      LocalDate date,
      boolean overdue,
      ChecklistAlert checklistItem,
      SuggestionAlert suggestion) {}

  public record ChecklistAlert(long id, ChecklistCategory category, String title) {}

  public record SuggestionAlert(
      long id, String ruleCode, String logicalKey, Map<String, Object> params) {}
}
