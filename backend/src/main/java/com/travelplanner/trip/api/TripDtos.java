package com.travelplanner.trip.api;

import com.travelplanner.trip.domain.TripDetails;
import java.time.LocalDate;
import java.util.Locale;

final class TripDtos {

  static final String COLOR_PATTERN = "#[0-9A-Fa-f]{6}";

  private TripDtos() {}

  static TripDetails details(
      String name,
      LocalDate startDate,
      LocalDate endDate,
      EditableTripStatus status,
      String coverColor) {
    return new TripDetails(
        name.trim(),
        startDate,
        endDate,
        status.toDomain(),
        coverColor == null ? null : coverColor.toUpperCase(Locale.ROOT));
  }
}
