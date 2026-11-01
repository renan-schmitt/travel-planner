package com.travelplanner.trip.api;

import com.travelplanner.shared.validation.DateRange;
import com.travelplanner.trip.domain.TripDetails;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Edição da viagem (substitui todos os campos). {@code version} é a lida no {@code GET} (divergente
 * → 409); sem {@code coverColor}, mantém a cor atual.
 */
@DateRange
public record UpdateTripRequest(
    @NotNull Long version,
    @NotBlank @Size(max = 120) String name,
    LocalDate startDate,
    LocalDate endDate,
    @NotNull EditableTripStatus status,
    @Pattern(regexp = TripDtos.COLOR_PATTERN) String coverColor)
    implements DateRange.Bounded {

  TripDetails toDomain() {
    return TripDtos.details(name, startDate, endDate, status, coverColor);
  }
}
