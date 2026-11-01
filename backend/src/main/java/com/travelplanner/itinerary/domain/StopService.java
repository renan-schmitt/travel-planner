package com.travelplanner.itinerary.domain;

import com.travelplanner.itinerary.infra.StopRepository;
import com.travelplanner.shared.web.InvalidRequestException;
import com.travelplanner.shared.web.NotFoundException;
import com.travelplanner.trip.domain.TripAccessService;
import com.travelplanner.trip.domain.TripChangedEvent;
import com.travelplanner.trip.domain.TripRole;
import com.travelplanner.trip.domain.TripService;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Paradas da viagem. Leitura exige {@code VIEWER}; alterações, {@code EDITOR}. Toda alteração
 * recalcula as datas derivadas da viagem e publica {@link TripChangedEvent}.
 */
@Service
@RequiredArgsConstructor
public class StopService {

  private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM");

  private final StopRepository stops;
  private final TripAccessService access;
  private final TripService trips;
  private final ApplicationEventPublisher events;

  @Transactional(readOnly = true)
  public List<Stop> list(long tripId, long userId) {
    access.requireRole(tripId, userId, TripRole.VIEWER);
    return stops.findByTripIdOrderBySeq(tripId);
  }

  /**
   * Cria a parada na posição cronológica: depois da última parada que chega no mesmo dia ou antes.
   */
  @Transactional
  public Stop create(long tripId, long userId, StopDetails details) {
    access.requireRole(tripId, userId, TripRole.EDITOR);
    List<Stop> current = stops.findByTripIdOrderBySeq(tripId);
    checkOverlap(details, current, null);
    int position = 0;
    for (int i = 0; i < current.size(); i++) {
      if (!current.get(i).getArrivalDate().isAfter(details.arrivalDate())) {
        position = i + 1;
      }
    }
    Stop stop = new Stop(tripId, details);
    List<Stop> ordered = new ArrayList<>(current);
    ordered.add(position, stop);
    renumber(current, ordered);
    changed(tripId);
    return stop;
  }

  @Transactional
  public Stop update(long tripId, long stopId, long userId, long version, StopDetails details) {
    access.requireRole(tripId, userId, TripRole.EDITOR);
    Stop stop = find(tripId, stopId);
    if (stop.getVersion() != version) {
      throw new OptimisticLockingFailureException("Versão da parada divergente.");
    }
    checkOverlap(details, stops.findByTripIdOrderBySeq(tripId), stopId);
    stop.update(details);
    stops.saveAndFlush(stop);
    changed(tripId);
    return stop;
  }

  @Transactional
  public void delete(long tripId, long stopId, long userId) {
    access.requireRole(tripId, userId, TripRole.EDITOR);
    stops.delete(find(tripId, stopId));
    stops.flush();
    changed(tripId);
  }

  /** Nova ordem: {@code stopIds} precisa ter exatamente as paradas da viagem. */
  @Transactional
  public List<Stop> reorder(long tripId, long userId, List<Long> stopIds) {
    access.requireRole(tripId, userId, TripRole.EDITOR);
    List<Stop> current = stops.findByTripIdOrderBySeq(tripId);
    Map<Long, Stop> byId =
        current.stream().collect(Collectors.toMap(Stop::getId, Function.identity()));
    if (stopIds.size() != current.size()
        || new HashSet<>(stopIds).size() != stopIds.size()
        || !byId.keySet().containsAll(stopIds)) {
      throw new InvalidRequestException(
          "stopIds", "A lista precisa ter todas as paradas da viagem, sem repetição.");
    }
    List<Stop> ordered = stopIds.stream().map(byId::get).toList();
    renumber(current, ordered);
    changed(tripId);
    return ordered;
  }

  private Stop find(long tripId, long stopId) {
    return stops
        .findByIdAndTripId(stopId, tripId)
        .orElseThrow(() -> new NotFoundException("Parada não encontrada."));
  }

  private static void checkOverlap(StopDetails details, List<Stop> others, Long ignoreId) {
    others.stream()
        .filter(o -> !o.getId().equals(ignoreId))
        .filter(o -> details.overlapsMoreThanOneDay(o.getArrivalDate(), o.getDepartureDate()))
        .findFirst()
        .ifPresent(
            o -> {
              throw new InvalidRequestException(
                  "arrivalDate",
                  "As datas se sobrepõem à parada em %s (%s a %s)."
                      .formatted(
                          o.getCityName(),
                          DAY.format(o.getArrivalDate()),
                          DAY.format(o.getDepartureDate())));
            });
  }

  /**
   * Grava {@code seq} = 1..n na ordem dada. Por causa do {@code UNIQUE(trip_id, seq)}, as paradas
   * existentes passam antes por valores negativos (temporários), num flush separado.
   */
  private void renumber(List<Stop> existing, List<Stop> ordered) {
    for (int i = 0; i < existing.size(); i++) {
      existing.get(i).moveTo(-(i + 1));
    }
    stops.flush();
    for (int i = 0; i < ordered.size(); i++) {
      ordered.get(i).moveTo(i + 1);
    }
    stops.saveAll(ordered);
    stops.flush();
  }

  private void changed(long tripId) {
    trips.refreshItineraryDates(tripId);
    events.publishEvent(new TripChangedEvent(tripId));
  }
}
