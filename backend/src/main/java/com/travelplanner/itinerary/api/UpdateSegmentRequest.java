package com.travelplanner.itinerary.api;

import com.travelplanner.itinerary.domain.SegmentDetails;
import com.travelplanner.itinerary.domain.TransportType;
import com.travelplanner.shared.validation.TimeZoneId;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/** Edição do trecho (substitui todos os campos). {@code version} divergente → 409. */
public record UpdateSegmentRequest(
    @NotNull Long version,
    @NotNull TransportType type,
    Long originStopId,
    Long destinationStopId,
    Long localStopId,
    @Size(max = 160) String originLabel,
    @Size(max = 160) String destinationLabel,
    @NotNull LocalDateTime departAt,
    @NotBlank @Size(max = 64) @TimeZoneId String departTz,
    LocalDateTime arriveAt,
    @Size(max = 64) @TimeZoneId String arriveTz,
    @Size(max = 120) String carrier,
    @Size(max = 40) String code,
    @Size(max = 80) String bookingRef,
    @Size(max = 2000) String notes) {

  SegmentDetails toDomain() {
    return new SegmentRequest(
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
            notes)
        .toDomain();
  }
}
