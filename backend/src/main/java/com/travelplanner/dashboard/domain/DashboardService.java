package com.travelplanner.dashboard.domain;

import com.travelplanner.checklist.domain.ChecklistItem;
import com.travelplanner.checklist.domain.ChecklistService;
import com.travelplanner.dashboard.domain.Dashboard.Alert;
import com.travelplanner.dashboard.domain.Dashboard.AlertKind;
import com.travelplanner.dashboard.domain.Dashboard.ChecklistAlert;
import com.travelplanner.dashboard.domain.Dashboard.SuggestionAlert;
import com.travelplanner.dashboard.domain.Dashboard.UpcomingTrip;
import com.travelplanner.suggestion.domain.Severity;
import com.travelplanner.suggestion.domain.SuggestionService;
import com.travelplanner.suggestion.domain.SuggestionService.SuggestionView;
import com.travelplanner.trip.domain.Trip;
import com.travelplanner.trip.domain.TripScope;
import com.travelplanner.trip.domain.TripService;
import com.travelplanner.trip.domain.TripStatus;
import com.travelplanner.trip.domain.TripView;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Monta o dashboard do usuário com uma consulta por tipo de dado (viagens, paradas, progresso do
 * checklist, sugestões por severidade, itens com prazo, sugestões críticas), sem N+1.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

  /** Janela dos alertas de checklist: prazo vencido ou nos próximos 14 dias. */
  static final int ALERT_WINDOW_DAYS = 14;

  static final int MAX_ALERTS = 10;
  static final int MAX_PAST_TRIPS = 5;

  /** Vencidos primeiro, depois os mais graves, depois a data mais próxima (sem data por último). */
  private static final Comparator<Alert> ALERT_ORDER =
      Comparator.comparing(Alert::overdue)
          .reversed()
          .thenComparing(Alert::severity, Comparator.reverseOrder())
          .thenComparing(Alert::date, Comparator.nullsLast(Comparator.naturalOrder()))
          .thenComparing(Alert::tripId);

  private final TripService trips;
  private final ChecklistService checklist;
  private final SuggestionService suggestions;

  @Transactional(readOnly = true)
  public Dashboard get(long userId) {
    LocalDate today = trips.today(userId);
    List<TripView> all = trips.list(userId, TripScope.ALL);
    List<TripView> upcoming = all.stream().filter(v -> v.status() != TripStatus.DONE).toList();
    List<TripView> past =
        all.stream()
            .filter(v -> v.status() == TripStatus.DONE)
            .sorted(
                Comparator.comparing(
                    (TripView v) -> v.trip().getEndDate(),
                    Comparator.nullsLast(Comparator.<LocalDate>reverseOrder())))
            .limit(MAX_PAST_TRIPS)
            .toList();

    List<Long> ids = upcoming.stream().map(v -> v.trip().getId()).toList();
    Map<Long, ChecklistService.Progress> progress = checklist.progress(ids);
    Map<Long, Map<Severity, Integer>> pending = suggestions.countPendingBySeverity(ids, today);

    List<UpcomingTrip> cards =
        upcoming.stream()
            .map(
                v -> {
                  long id = v.trip().getId();
                  var p = progress.getOrDefault(id, new ChecklistService.Progress(0, 0));
                  return new UpcomingTrip(
                      v,
                      daysUntil(v.trip().getStartDate(), today),
                      p.done(),
                      p.total(),
                      bySeverity(pending.get(id)));
                })
            .toList();

    return new Dashboard(cards, alerts(upcoming, ids, today), past);
  }

  private List<Alert> alerts(List<TripView> upcoming, List<Long> ids, LocalDate today) {
    Map<Long, Trip> tripsById =
        upcoming.stream()
            .map(TripView::trip)
            .collect(Collectors.toMap(Trip::getId, Function.identity()));
    Stream<Alert> items =
        checklist.openDueUntil(ids, today.plusDays(ALERT_WINDOW_DAYS)).stream()
            .map(item -> checklistAlert(item, tripsById.get(item.getTripId()), today));
    Stream<Alert> critical =
        suggestions.pending(ids, Severity.CRITICAL, today).stream()
            .map(s -> suggestionAlert(s, tripsById.get(s.suggestion().getTripId())));
    return Stream.concat(items, critical).sorted(ALERT_ORDER).limit(MAX_ALERTS).toList();
  }

  /** Item de checklist: atenção; o vencido vai para o topo ({@code overdue}). */
  private static Alert checklistAlert(ChecklistItem item, Trip trip, LocalDate today) {
    return new Alert(
        AlertKind.CHECKLIST_ITEM,
        trip.getId(),
        trip.getName(),
        Severity.WARNING,
        item.getDueDate(),
        item.getDueDate().isBefore(today),
        new ChecklistAlert(item.getId(), item.getCategory(), item.getTitle()),
        null);
  }

  private static Alert suggestionAlert(SuggestionView view, Trip trip) {
    var s = view.suggestion();
    return new Alert(
        AlertKind.SUGGESTION,
        trip.getId(),
        trip.getName(),
        s.getSeverity(),
        trip.getStartDate(),
        false,
        null,
        new SuggestionAlert(
            s.getId(), s.getRuleCode(), s.getLogicalKey(), view.payload().params()));
  }

  private static Integer daysUntil(LocalDate start, LocalDate today) {
    return start == null ? null : (int) ChronoUnit.DAYS.between(today, start);
  }

  /** Todas as severidades presentes no mapa (zero quando não há pendentes). */
  private static Map<Severity, Integer> bySeverity(Map<Severity, Integer> counts) {
    Map<Severity, Integer> result = new EnumMap<>(Severity.class);
    for (Severity s : Severity.values()) {
      result.put(s, counts == null ? 0 : counts.getOrDefault(s, 0));
    }
    return result;
  }
}
