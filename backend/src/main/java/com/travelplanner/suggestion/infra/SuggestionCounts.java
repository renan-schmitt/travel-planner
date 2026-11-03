package com.travelplanner.suggestion.infra;

import com.travelplanner.trip.domain.TripSuggestions;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Implementa {@link TripSuggestions} para o módulo {@code trip} (badge no card da viagem). */
@Component
@RequiredArgsConstructor
class SuggestionCounts implements TripSuggestions {

  private final SuggestionRepository suggestions;

  @Override
  public Map<Long, Integer> countPending(Collection<Long> tripIds, LocalDate today) {
    Map<Long, Integer> counts = new HashMap<>();
    if (tripIds.isEmpty()) {
      return counts;
    }
    for (Object[] row : suggestions.countPending(tripIds, today)) {
      counts.put((Long) row[0], ((Number) row[1]).intValue());
    }
    return counts;
  }
}
