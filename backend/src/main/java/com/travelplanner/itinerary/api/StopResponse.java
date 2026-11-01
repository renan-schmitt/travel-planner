package com.travelplanner.itinerary.api;

import com.travelplanner.itinerary.domain.Stop;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Parada do roteiro. {@code nights} = dias entre chegada e saída (0 num bate-volta). */
public record StopResponse(
    @NotNull Long id,
    @NotNull Integer seq,
    String cityPlaceId,
    @NotNull String cityName,
    @NotNull String countryCode,
    Double lat,
    Double lng,
    @NotNull String timezone,
    @NotNull LocalDate arrivalDate,
    @NotNull LocalDate departureDate,
    @NotNull Integer nights,
    String notes,
    @NotNull Long version) {

  static StopResponse from(Stop stop) {
    return new StopResponse(
        stop.getId(),
        stop.getSeq(),
        stop.getCityPlaceId(),
        stop.getCityName(),
        stop.getCountryCode(),
        toDouble(stop.getLat()),
        toDouble(stop.getLng()),
        stop.getTimezone(),
        stop.getArrivalDate(),
        stop.getDepartureDate(),
        stop.nights(),
        stop.getNotes(),
        stop.getVersion());
  }

  private static Double toDouble(BigDecimal value) {
    return value == null ? null : value.doubleValue();
  }
}
