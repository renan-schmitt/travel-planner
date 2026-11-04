package com.travelplanner.itinerary.domain;

import com.travelplanner.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Atividade planejada num dia da viagem. Sem horário, a posição no dia é {@code sortOrder}; com
 * horário, a timeline ordena pelo horário.
 */
@Getter
@Entity
@Table(name = "activity")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Activity extends BaseEntity {

  @Column(name = "trip_id", nullable = false, updatable = false)
  private Long tripId;

  @Column(nullable = false)
  private LocalDate day;

  @Column(name = "start_time")
  private LocalTime startTime;

  @Column(name = "end_time")
  private LocalTime endTime;

  @Column(nullable = false, length = 200)
  private String title;

  @Column(columnDefinition = "text")
  private String notes;

  @Column(name = "saved_place_id")
  private Long savedPlaceId;

  @Column(name = "sort_order", nullable = false)
  private int sortOrder;

  public Activity(Long tripId, ActivityDetails details, int sortOrder) {
    this.tripId = tripId;
    update(details);
    this.sortOrder = sortOrder;
  }

  public void update(ActivityDetails details) {
    this.day = details.day();
    this.startTime = details.startTime();
    this.endTime = details.endTime();
    this.title = details.title();
    this.notes = details.notes();
    this.savedPlaceId = details.savedPlaceId();
  }

  /** Move para {@code day} na posição {@code sortOrder} (arrastar e soltar). */
  public void moveTo(LocalDate day, int sortOrder) {
    this.day = day;
    this.sortOrder = sortOrder;
  }

  public ActivityDetails details() {
    return new ActivityDetails(day, startTime, endTime, title, notes, savedPlaceId);
  }
}
