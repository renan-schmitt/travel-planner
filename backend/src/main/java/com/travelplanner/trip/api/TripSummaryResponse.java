package com.travelplanner.trip.api;

import com.travelplanner.trip.domain.Trip;
import com.travelplanner.trip.domain.TripRole;
import com.travelplanner.trip.domain.TripStatus;
import com.travelplanner.trip.domain.TripView;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * Item da listagem. {@code status} é o efetivo (inclui {@code IN_PROGRESS}/{@code DONE}); {@code
 * destinationCount} fica 0 até as paradas existirem (Fase 7).
 */
public record TripSummaryResponse(
    @NotNull Long id,
    @NotNull String name,
    LocalDate startDate,
    LocalDate endDate,
    @NotNull TripStatus status,
    String coverColor,
    @NotNull TripRole role,
    @NotNull Integer destinationCount) {

  static TripSummaryResponse from(TripView view) {
    Trip trip = view.trip();
    return new TripSummaryResponse(
        trip.getId(),
        trip.getName(),
        trip.getStartDate(),
        trip.getEndDate(),
        view.status(),
        trip.getCoverColor(),
        view.role(),
        0);
  }
}
