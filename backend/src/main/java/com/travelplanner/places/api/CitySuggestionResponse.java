package com.travelplanner.places.api;

import com.travelplanner.places.domain.CitySuggestion;
import jakarta.validation.constraints.NotNull;

/** Sugestão do autocomplete: {@code name} ("Lisboa") e {@code description} ("Portugal"). */
public record CitySuggestionResponse(
    @NotNull String placeId, @NotNull String name, String description) {

  static CitySuggestionResponse from(CitySuggestion suggestion) {
    return new CitySuggestionResponse(
        suggestion.placeId(), suggestion.name(), suggestion.description());
  }
}
