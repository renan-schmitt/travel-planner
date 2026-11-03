package com.travelplanner.dashboard.api;

import com.travelplanner.checklist.domain.ChecklistCategory;
import com.travelplanner.dashboard.domain.Dashboard;
import com.travelplanner.dashboard.domain.Dashboard.AlertKind;
import com.travelplanner.suggestion.domain.Severity;
import com.travelplanner.trip.domain.Trip;
import com.travelplanner.trip.domain.TripRole;
import com.travelplanner.trip.domain.TripStatus;
import com.travelplanner.trip.domain.TripView;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Dashboard. {@code upcomingTrips}: viagens não encerradas, em ordem de início; {@code alerts}: até
 * 10, vencidos primeiro, depois por severidade e data; {@code pastTrips}: as 5 mais recentes.
 */
public record DashboardResponse(
    @NotNull List<UpcomingTripResponse> upcomingTrips,
    @NotNull List<AlertResponse> alerts,
    @NotNull List<PastTripResponse> pastTrips) {

  /**
   * {@code daysUntilStart}: 0 = hoje, negativo = já começou, nulo sem data. {@code
   * pendingSuggestions}: sugestões pendentes por severidade.
   */
  public record UpcomingTripResponse(
      @NotNull Long id,
      @NotNull String name,
      LocalDate startDate,
      LocalDate endDate,
      @NotNull TripStatus status,
      String coverColor,
      @NotNull TripRole role,
      @NotNull Integer destinationCount,
      Integer daysUntilStart,
      @NotNull Integer checklistDone,
      @NotNull Integer checklistTotal,
      @NotNull PendingSuggestions pendingSuggestions) {}

  public record PendingSuggestions(
      @NotNull Integer critical, @NotNull Integer warning, @NotNull Integer info) {

    static PendingSuggestions from(Map<Severity, Integer> counts) {
      return new PendingSuggestions(
          counts.get(Severity.CRITICAL), counts.get(Severity.WARNING), counts.get(Severity.INFO));
    }
  }

  /**
   * Alerta: {@code checklistItem} preenchido quando {@code kind = CHECKLIST_ITEM}, {@code
   * suggestion} quando {@code SUGGESTION} (o texto da sugestão sai do i18n pelos {@code params}).
   */
  public record AlertResponse(
      @NotNull AlertKind kind,
      @NotNull Long tripId,
      @NotNull String tripName,
      @NotNull Severity severity,
      LocalDate date,
      @NotNull Boolean overdue,
      ChecklistItemAlert checklistItem,
      SuggestionAlert suggestion) {}

  public record ChecklistItemAlert(
      @NotNull Long id, @NotNull ChecklistCategory category, @NotNull String title) {}

  public record SuggestionAlert(
      @NotNull Long id,
      @NotNull String ruleCode,
      @NotNull String logicalKey,
      @NotNull Map<String, Object> params) {}

  public record PastTripResponse(
      @NotNull Long id,
      @NotNull String name,
      LocalDate startDate,
      LocalDate endDate,
      String coverColor,
      @NotNull Integer destinationCount) {}

  static DashboardResponse from(Dashboard dashboard) {
    return new DashboardResponse(
        dashboard.upcomingTrips().stream().map(DashboardResponse::upcoming).toList(),
        dashboard.alerts().stream().map(DashboardResponse::alert).toList(),
        dashboard.pastTrips().stream().map(DashboardResponse::past).toList());
  }

  private static UpcomingTripResponse upcoming(Dashboard.UpcomingTrip card) {
    TripView view = card.view();
    Trip trip = view.trip();
    return new UpcomingTripResponse(
        trip.getId(),
        trip.getName(),
        trip.getStartDate(),
        trip.getEndDate(),
        view.status(),
        trip.getCoverColor(),
        view.role(),
        view.destinationCount(),
        card.daysUntilStart(),
        card.checklistDone(),
        card.checklistTotal(),
        PendingSuggestions.from(card.pendingSuggestions()));
  }

  private static AlertResponse alert(Dashboard.Alert alert) {
    var item = alert.checklistItem();
    var suggestion = alert.suggestion();
    return new AlertResponse(
        alert.kind(),
        alert.tripId(),
        alert.tripName(),
        alert.severity(),
        alert.date(),
        alert.overdue(),
        item == null ? null : new ChecklistItemAlert(item.id(), item.category(), item.title()),
        suggestion == null
            ? null
            : new SuggestionAlert(
                suggestion.id(),
                suggestion.ruleCode(),
                suggestion.logicalKey(),
                suggestion.params()));
  }

  private static PastTripResponse past(TripView view) {
    Trip trip = view.trip();
    return new PastTripResponse(
        trip.getId(),
        trip.getName(),
        trip.getStartDate(),
        trip.getEndDate(),
        trip.getCoverColor(),
        view.destinationCount());
  }
}
