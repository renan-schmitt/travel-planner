package com.travelplanner.itinerary.api;

import com.travelplanner.itinerary.domain.ActivityDetails;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Nova atividade. Horários "HH:mm" opcionais ({@code endTime} só com {@code startTime}); {@code
 * title} é opcional quando há {@code savedPlaceId} (vira o nome do favorito).
 */
public record ActivityRequest(
    @NotNull LocalDate day,
    @Pattern(regexp = ActivityRequest.TIME) String startTime,
    @Pattern(regexp = ActivityRequest.TIME) String endTime,
    @Size(max = 200) String title,
    @Size(max = 2000) String notes,
    Long savedPlaceId) {

  static final String TIME = "([01]\\d|2[0-3]):[0-5]\\d";

  ActivityDetails toDomain() {
    return details(day, startTime, endTime, title, notes, savedPlaceId);
  }

  static ActivityDetails details(
      LocalDate day,
      String startTime,
      String endTime,
      String title,
      String notes,
      Long savedPlaceId) {
    return new ActivityDetails(
        day,
        time(startTime),
        time(endTime),
        SegmentRequest.clean(title),
        SegmentRequest.clean(notes),
        savedPlaceId);
  }

  private static LocalTime time(String value) {
    return value == null || value.isBlank() ? null : LocalTime.parse(value);
  }
}
