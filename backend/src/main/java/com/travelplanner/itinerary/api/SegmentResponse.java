package com.travelplanner.itinerary.api;

import com.travelplanner.itinerary.domain.SegmentDetails;
import com.travelplanner.itinerary.domain.TransportSegment;
import com.travelplanner.itinerary.domain.TransportType;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

/**
 * Trecho de transporte. Além dos horários locais e fusos de cada ponta, traz os instantes em
 * ISO-8601 com o offset da ponta ({@code departInstant}: {@code 2027-03-10T22:40:00-03:00}).
 */
public record SegmentResponse(
    @NotNull Long id,
    @NotNull TransportType type,
    @NotNull Boolean local,
    Long originStopId,
    Long destinationStopId,
    Long localStopId,
    String originLabel,
    String destinationLabel,
    @NotNull LocalDateTime departAt,
    @NotNull String departTz,
    @NotNull OffsetDateTime departInstant,
    LocalDateTime arriveAt,
    String arriveTz,
    OffsetDateTime arriveInstant,
    String carrier,
    String code,
    String bookingRef,
    String notes,
    @NotNull Long version) {

  static SegmentResponse from(TransportSegment segment) {
    SegmentDetails d = segment.details();
    return new SegmentResponse(
        segment.getId(),
        d.type(),
        d.local(),
        d.originStopId(),
        d.destinationStopId(),
        d.localStopId(),
        d.originLabel(),
        d.destinationLabel(),
        d.departAt(),
        d.departTz(),
        d.departure().toOffsetDateTime(),
        d.arriveAt(),
        d.arriveTz(),
        d.arriveAt() == null ? null : d.arrival().toOffsetDateTime(),
        d.carrier(),
        d.code(),
        d.bookingRef(),
        d.notes(),
        segment.getVersion());
  }
}
