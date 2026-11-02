package com.travelplanner.itinerary.infra;

import com.travelplanner.trip.domain.DateSpan;
import com.travelplanner.trip.domain.TripItinerary;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * O roteiro visto pelo módulo {@code trip}: contagem de destinos e período coberto pelas paradas e
 * pelos trechos de transporte (data local da primeira partida e da última chegada).
 */
@Component
@RequiredArgsConstructor
class StopItinerary implements TripItinerary {

  private final StopRepository stops;
  private final TransportSegmentRepository segments;

  @Override
  public Map<Long, Integer> countDestinations(Collection<Long> tripIds) {
    if (tripIds.isEmpty()) {
      return Map.of();
    }
    return stops.countByTripIds(tripIds).stream()
        .collect(
            Collectors.toMap(
                row -> ((Number) row[0]).longValue(), row -> ((Number) row[1]).intValue()));
  }

  @Override
  public DateSpan dateSpan(long tripId) {
    DateSpan fromStops = stops.dateSpan(tripId);
    SegmentSpan fromSegments = segments.span(tripId);
    LocalDate start = fromStops == null ? null : fromStops.start();
    LocalDate end = fromStops == null ? null : fromStops.end();
    if (fromSegments != null && fromSegments.first() != null) {
      start = earliest(start, fromSegments.first().toLocalDate());
      end = latest(end, fromSegments.last().toLocalDate());
    }
    return new DateSpan(start, end);
  }

  private static LocalDate earliest(LocalDate a, LocalDate b) {
    return a == null || b.isBefore(a) ? b : a;
  }

  private static LocalDate latest(LocalDate a, LocalDate b) {
    return a == null || b.isAfter(a) ? b : a;
  }
}
