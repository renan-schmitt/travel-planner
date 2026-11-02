package com.travelplanner.trip.api;

import com.travelplanner.trip.domain.JoinResult;
import com.travelplanner.trip.domain.TripRole;
import jakarta.validation.constraints.NotNull;

/** Viagem em que o usuário entrou pelo link e o papel dele nela. */
public record JoinResponse(@NotNull Long tripId, @NotNull TripRole role) {

  static JoinResponse from(JoinResult result) {
    return new JoinResponse(result.tripId(), result.role());
  }
}
