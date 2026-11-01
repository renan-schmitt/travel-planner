package com.travelplanner.itinerary.api;

import com.travelplanner.itinerary.domain.StopDetails;
import com.travelplanner.shared.validation.TimeZoneId;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Edição da parada (substitui todos os campos). {@code version} divergente → 409. */
@StopDates
public record UpdateStopRequest(
    @NotNull Long version,
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
    return StopRequest.toDetails(
        cityPlaceId, cityName, countryCode, lat, lng, timezone, arrivalDate, departureDate, notes);
  }
}
