package com.travelplanner.itinerary.api;

import com.travelplanner.itinerary.domain.StopDetails;
import com.travelplanner.shared.validation.TimeZoneId;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Nova parada. Com a cidade do autocomplete vêm {@code cityPlaceId}, {@code lat} e {@code lng};
 * digitada à mão, só nome, país e fuso. Saída ≥ chegada (validada pela {@link StopDates}).
 */
@StopDates
public record StopRequest(
    @Size(max = 255) String cityPlaceId,
    @NotBlank @Size(max = 160) String cityName,
    @NotNull @Pattern(regexp = "[A-Z]{2}") String countryCode,
    @DecimalMin("-90") @DecimalMax("90") Double lat,
    @DecimalMin("-180") @DecimalMax("180") Double lng,
    @NotBlank @Size(max = 64) @TimeZoneId String timezone,
    @NotNull LocalDate arrivalDate,
    @NotNull LocalDate departureDate,
    @Size(max = 2000) String notes)
    implements StopDates.Bounded {

  StopDetails toDomain() {
    return toDetails(
        cityPlaceId, cityName, countryCode, lat, lng, timezone, arrivalDate, departureDate, notes);
  }

  static StopDetails toDetails(
      String cityPlaceId,
      String cityName,
      String countryCode,
      Double lat,
      Double lng,
      String timezone,
      LocalDate arrivalDate,
      LocalDate departureDate,
      String notes) {
    boolean hasCoordinates = lat != null && lng != null;
    return new StopDetails(
        cityPlaceId == null || cityPlaceId.isBlank() ? null : cityPlaceId,
        cityName.strip(),
        countryCode,
        hasCoordinates ? coordinate(lat) : null,
        hasCoordinates ? coordinate(lng) : null,
        timezone,
        arrivalDate,
        departureDate,
        notes == null || notes.isBlank() ? null : notes.strip());
  }

  private static BigDecimal coordinate(double value) {
    return BigDecimal.valueOf(value).setScale(6, RoundingMode.HALF_UP);
  }
}
