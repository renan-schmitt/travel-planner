package com.travelplanner.suggestion.domain.rules;

import com.travelplanner.itinerary.domain.SavedPlaceKind;
import com.travelplanner.itinerary.domain.TripSnapshot.StopEntry;
import com.travelplanner.suggestion.domain.GapContext;
import com.travelplanner.suggestion.domain.GapRule;
import com.travelplanner.suggestion.domain.Severity;
import com.travelplanner.suggestion.domain.SuggestionDraft;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * Parada com pelo menos {@code minNights} noites e nenhum favorito do tipo {@code kind}: INFO, com
 * a chave = id da parada. Base de {@code RESTAURANTES} e {@code ATRACOES}.
 */
abstract class MissingFavoritesRule implements GapRule {

  private final SavedPlaceKind kind;
  private final int minNights;

  MissingFavoritesRule(SavedPlaceKind kind, int minNights) {
    this.kind = kind;
    this.minNights = minNights;
  }

  @Override
  public List<SuggestionDraft> evaluate(GapContext context) {
    return context.snapshot().stops().stream()
        .filter(stop -> nights(stop) >= minNights)
        .filter(stop -> !hasFavorite(context, stop))
        .map(
            stop ->
                SuggestionDraft.of(
                    String.valueOf(stop.id()),
                    Severity.INFO,
                    Map.of(
                        "stopId", stop.id(),
                        "city", stop.details().cityName(),
                        "nights", nights(stop))))
        .toList();
  }

  private boolean hasFavorite(GapContext context, StopEntry stop) {
    return context.snapshot().savedPlaces().stream()
        .anyMatch(p -> p.stopId() == stop.id() && p.kind() == kind);
  }

  private static int nights(StopEntry stop) {
    return (int)
        ChronoUnit.DAYS.between(stop.details().arrivalDate(), stop.details().departureDate());
  }
}
