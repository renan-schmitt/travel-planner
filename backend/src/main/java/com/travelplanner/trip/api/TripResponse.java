package com.travelplanner.trip.api;

import com.travelplanner.trip.domain.Trip;
import com.travelplanner.trip.domain.TripRole;
import com.travelplanner.trip.domain.TripStatus;
import com.travelplanner.trip.domain.TripView;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * Detalhe da viagem (cresce nas próximas fases). {@code status} é o efetivo; {@code plannedStatus}
 * é o gravado ({@code PLANNING}/{@code CONFIRMED}), usado no formulário de edição. Com {@code
 * datesFromStops}, as datas não foram digitadas: vêm das paradas (podem ser nulas).
 */
public record TripResponse(
    @NotNull Long id,
    @NotNull String name,
    LocalDate startDate,
    LocalDate endDate,
    @NotNull Boolean datesFromStops,
    @NotNull TripStatus status,
    @NotNull EditableTripStatus plannedStatus,
    String coverColor,
    @NotNull TripRole role,
    @NotNull Integer destinationCount,
    @NotNull Long version) {

  static TripResponse from(TripView view) {
    Trip trip = view.trip();
    return new TripResponse(
        trip.getId(),
        trip.getName(),
        trip.getStartDate(),
        trip.getEndDate(),
        !trip.isDatesManual(),
        view.status(),
        EditableTripStatus.valueOf(trip.getPlannedStatus().name()),
        trip.getCoverColor(),
        view.role(),
        view.destinationCount(),
        trip.getVersion());
  }
}
