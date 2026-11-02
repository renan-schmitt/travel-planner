package com.travelplanner.itinerary.api;

import com.travelplanner.itinerary.domain.SegmentDetails;
import com.travelplanner.itinerary.domain.TransportType;
import com.travelplanner.shared.validation.TimeZoneId;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * Novo trecho. De deslocamento: {@code originStopId} → {@code destinationStopId} ({@code null} =
 * casa). Local: só {@code localStopId}. Horários locais ({@code 2027-03-10T22:40}) com o fuso de
 * cada ponta; chegada opcional. As regras entre campos ficam em {@link SegmentDetails#validate}.
 */
public record SegmentRequest(
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
    return new SegmentDetails(
        type,
        originStopId,
        destinationStopId,
        localStopId,
        clean(originLabel),
        clean(destinationLabel),
        departAt,
        departTz,
        arriveAt,
        arriveAt == null ? null : clean(arriveTz),
        clean(carrier),
        clean(code),
        clean(bookingRef),
        clean(notes));
  }

  static String clean(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }
}
