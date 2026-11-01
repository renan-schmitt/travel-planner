package com.travelplanner.trip.domain;

import java.time.LocalDate;

/**
 * Campos editáveis da viagem. {@code status} só aceita {@code PLANNING} ou {@code CONFIRMED};
 * {@code coverColor} nulo na criação recebe uma cor da paleta.
 */
public record TripDetails(
    String name, LocalDate startDate, LocalDate endDate, TripStatus status, String coverColor) {

  public TripDetails {
    if (status != TripStatus.PLANNING && status != TripStatus.CONFIRMED) {
      throw new IllegalArgumentException("Status calculado não pode ser gravado: " + status);
    }
    if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
      throw new IllegalArgumentException("A data de fim é anterior à de início.");
    }
  }

  public TripDetails withCoverColor(String color) {
    return new TripDetails(name, startDate, endDate, status, color);
  }
}
