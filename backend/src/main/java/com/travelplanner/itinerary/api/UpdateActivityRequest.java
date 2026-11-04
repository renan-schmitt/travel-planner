package com.travelplanner.itinerary.api;

import com.travelplanner.itinerary.domain.ActivityDetails;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Edição da atividade (substitui todos os campos). {@code version} divergente → 409. */
public record UpdateActivityRequest(
    @NotNull Long version,
    @NotNull LocalDate day,
    @Pattern(regexp = ActivityRequest.TIME) String startTime,
    @Pattern(regexp = ActivityRequest.TIME) String endTime,
    @Size(max = 200) String title,
    @Size(max = 2000) String notes,
    Long savedPlaceId) {

  ActivityDetails toDomain() {
    return ActivityRequest.details(day, startTime, endTime, title, notes, savedPlaceId);
  }
}
