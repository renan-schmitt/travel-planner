package com.travelplanner.itinerary.infra;

import com.travelplanner.trip.domain.DateSpan;
import com.travelplanner.trip.domain.TripItinerary;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** O roteiro visto pelo módulo {@code trip}: contagem de destinos e período das paradas. */
@Component
@RequiredArgsConstructor
class StopItinerary implements TripItinerary {

  private final StopRepository stops;

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
    DateSpan span = stops.dateSpan(tripId);
    return span == null ? new DateSpan(null, null) : span;
  }
}
