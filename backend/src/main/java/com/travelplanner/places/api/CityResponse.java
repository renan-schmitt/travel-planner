package com.travelplanner.places.api;

import com.travelplanner.places.domain.City;
import jakarta.validation.constraints.NotNull;

/**
 * Cidade escolhida. {@code countryCode} e {@code timezone} podem faltar (o usuário completa à mão).
 */
public record CityResponse(
    @NotNull String placeId,
    @NotNull String name,
    String countryCode,
    @NotNull Double lat,
    @NotNull Double lng,
    String timezone) {

  static CityResponse from(City city) {
    return new CityResponse(
        city.placeId(), city.name(), city.countryCode(), city.lat(), city.lng(), city.timezone());
  }
}
