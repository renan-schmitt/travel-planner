package com.travelplanner.trip.api;

import com.travelplanner.trip.domain.TripStatus;

/** Status que o usuário pode escolher; {@code IN_PROGRESS} e {@code DONE} são calculados. */
public enum EditableTripStatus {
  PLANNING,
  CONFIRMED;

  TripStatus toDomain() {
    return TripStatus.valueOf(name());
  }
}
