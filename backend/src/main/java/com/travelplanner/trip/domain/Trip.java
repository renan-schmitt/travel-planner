package com.travelplanner.trip.domain;

import com.travelplanner.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Viagem. O acesso é controlado pelos {@link TripMember membros} (ver {@link TripAccessService}).
 */
@Getter
@Entity
@Table(name = "trip")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Trip extends BaseEntity {

  @Column(nullable = false, length = 120)
  private String name;

  @Column(name = "start_date")
  private LocalDate startDate;

  @Column(name = "end_date")
  private LocalDate endDate;

  /** Status gravado ({@code PLANNING} ou {@code CONFIRMED}); o exibido vem de {@link #status}. */
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private TripStatus plannedStatus = TripStatus.PLANNING;

  @Column(name = "cover_color", length = 16)
  private String coverColor;

  @Column(name = "owner_id", nullable = false, updatable = false)
  private Long ownerId;

  public Trip(Long ownerId, TripDetails details) {
    this.ownerId = ownerId;
    update(details);
  }

  public void update(TripDetails details) {
    this.name = details.name();
    this.startDate = details.startDate();
    this.endDate = details.endDate();
    this.plannedStatus = details.status();
    this.coverColor = details.coverColor();
  }

  /** Status efetivo em {@code today} (ver {@link TripStatus#effective}). */
  public TripStatus status(LocalDate today) {
    return TripStatus.effective(plannedStatus, startDate, endDate, today);
  }
}
