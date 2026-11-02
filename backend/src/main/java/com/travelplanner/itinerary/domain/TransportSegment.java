package com.travelplanner.itinerary.domain;

import com.travelplanner.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Trecho de transporte da viagem (ver {@link SegmentDetails}). */
@Getter
@Entity
@Table(name = "transport_segment")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TransportSegment extends BaseEntity {

  @Column(name = "trip_id", nullable = false, updatable = false)
  private Long tripId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TransportType type;

  @Column(name = "origin_stop_id")
  private Long originStopId;

  @Column(name = "destination_stop_id")
  private Long destinationStopId;

  @Column(name = "local_stop_id")
  private Long localStopId;

  @Column(name = "origin_label", length = 160)
  private String originLabel;

  @Column(name = "destination_label", length = 160)
  private String destinationLabel;

  @Column(name = "depart_at", nullable = false)
  private LocalDateTime departAt;

  @Column(name = "depart_tz", nullable = false, length = 64)
  private String departTz;

  @Column(name = "arrive_at")
  private LocalDateTime arriveAt;

  @Column(name = "arrive_tz", length = 64)
  private String arriveTz;

  @Column(length = 120)
  private String carrier;

  @Column(length = 40)
  private String code;

  @Column(name = "booking_ref", length = 80)
  private String bookingRef;

  @Column(columnDefinition = "text")
  private String notes;

  public TransportSegment(Long tripId, SegmentDetails details) {
    this.tripId = tripId;
    update(details);
  }

  public void update(SegmentDetails details) {
    this.type = details.type();
    this.originStopId = details.originStopId();
    this.destinationStopId = details.destinationStopId();
    this.localStopId = details.localStopId();
    this.originLabel = details.originLabel();
    this.destinationLabel = details.destinationLabel();
    this.departAt = details.departAt();
    this.departTz = details.departTz();
    this.arriveAt = details.arriveAt();
    this.arriveTz = details.arriveTz();
    this.carrier = details.carrier();
    this.code = details.code();
    this.bookingRef = details.bookingRef();
    this.notes = details.notes();
  }

  public SegmentDetails details() {
    return new SegmentDetails(
        type,
        originStopId,
        destinationStopId,
        localStopId,
        originLabel,
        destinationLabel,
        departAt,
        departTz,
        arriveAt,
        arriveTz,
        carrier,
        code,
        bookingRef,
        notes);
  }
}
