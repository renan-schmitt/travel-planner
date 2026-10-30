package com.travelplanner.auth.api;

import com.travelplanner.auth.domain.HomeCity;
import com.travelplanner.shared.validation.TimeZoneId;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Cidade de origem. {@code placeId} e {@code timezone} chegam com o autocomplete (Fase 7). */
public record HomeCityDto(
    @NotBlank @Size(max = 255) String name,
    @Size(max = 255) String placeId,
    @Pattern(regexp = "[A-Z]{2}") String countryCode,
    @Size(max = 64) @TimeZoneId String timezone) {

  static HomeCityDto from(HomeCity city) {
    return city == null
        ? null
        : new HomeCityDto(city.name(), city.placeId(), city.countryCode(), city.timezone());
  }

  HomeCity toDomain() {
    return new HomeCity(name.trim(), placeId, countryCode, timezone);
  }
}
