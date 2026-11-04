package com.travelplanner.itinerary.api;

import com.travelplanner.itinerary.domain.Activity;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** Atividade; horários "HH:mm" no relógio local do dia. */
public record ActivityResponse(
    @NotNull Long id,
    @NotNull LocalDate day,
    String startTime,
    String endTime,
    @NotNull String title,
    String notes,
    Long savedPlaceId,
    @NotNull Integer sortOrder,
    @NotNull Long version) {

  private static final DateTimeFormatter HOUR = DateTimeFormatter.ofPattern("HH:mm");

  static ActivityResponse from(Activity activity) {
    return new ActivityResponse(
        activity.getId(),
        activity.getDay(),
        hour(activity.getStartTime()),
        hour(activity.getEndTime()),
        activity.getTitle(),
        activity.getNotes(),
        activity.getSavedPlaceId(),
        activity.getSortOrder(),
        activity.getVersion());
  }

  static String hour(LocalTime time) {
    return time == null ? null : HOUR.format(time);
  }
}
