package com.travelplanner.trip.api;

import com.travelplanner.trip.domain.AgeGroup;
import com.travelplanner.trip.domain.TravelerDetails;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/**
 * Novo viajante. {@code nationalities}: códigos ISO 3166-1 alfa-2 (a primeira é a principal); sem
 * número de passaporte, só a validade.
 */
public record TravelerRequest(
    @NotBlank @Size(max = 120) String name,
    @NotNull AgeGroup ageGroup,
    @Min(1900) @Max(2100) Integer birthYear,
    @NotEmpty @Size(max = 10) List<@NotNull @Pattern(regexp = "[A-Z]{2}") String> nationalities,
    LocalDate passportExpiry) {

  TravelerDetails toDomain() {
    return new TravelerDetails(name.strip(), ageGroup, birthYear, nationalities, passportExpiry);
  }
}
