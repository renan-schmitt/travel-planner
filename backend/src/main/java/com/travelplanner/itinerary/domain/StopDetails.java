package com.travelplanner.itinerary.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Campos editáveis de uma parada. Cidade do autocomplete tem {@code cityPlaceId} e coordenadas;
 * digitada à mão, só nome, país e fuso.
 */
public record StopDetails(
    String cityPlaceId,
    String cityName,
    String countryCode,
    BigDecimal lat,
    BigDecimal lng,
    String timezone,
    LocalDate arrivalDate,
    LocalDate departureDate,
    String notes) {

  public StopDetails {
    if (departureDate.isBefore(arrivalDate)) {
      throw new IllegalArgumentException("A saída é anterior à chegada.");
    }
  }

  /**
   * Duas paradas podem dividir no máximo um dia (o dia em que se troca de cidade): {@code Lisboa
   * 10–14} e {@code Porto 14–17} são válidas; {@code Lisboa 10–14} e {@code Porto 13–17} não.
   */
  public boolean overlapsMoreThanOneDay(LocalDate otherArrival, LocalDate otherDeparture) {
    LocalDate latestArrival = arrivalDate.isAfter(otherArrival) ? arrivalDate : otherArrival;
    LocalDate earliestDeparture =
        departureDate.isBefore(otherDeparture) ? departureDate : otherDeparture;
    return latestArrival.isBefore(earliestDeparture);
  }
}
