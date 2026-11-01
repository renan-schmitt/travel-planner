package com.travelplanner.trip.domain;

import java.time.LocalDate;
import java.util.List;

/**
 * Campos editáveis do viajante. {@code nationalities} são códigos ISO 3166-1 alfa-2, sem repetição
 * e em ordem (a primeira é a principal).
 */
public record TravelerDetails(
    String name,
    AgeGroup ageGroup,
    Integer birthYear,
    List<String> nationalities,
    LocalDate passportExpiry) {

  public TravelerDetails {
    if (nationalities == null || nationalities.isEmpty()) {
      throw new IllegalArgumentException("Informe ao menos uma nacionalidade.");
    }
    nationalities = nationalities.stream().distinct().toList();
  }
}
