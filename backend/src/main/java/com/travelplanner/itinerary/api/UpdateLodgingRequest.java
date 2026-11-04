package com.travelplanner.itinerary.api;

import com.travelplanner.itinerary.domain.BoardType;
import com.travelplanner.itinerary.domain.LodgingDetails;
import com.travelplanner.itinerary.domain.LodgingKind;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Edição da hospedagem (substitui todos os campos). {@code version} divergente → 409. */
public record UpdateLodgingRequest(
    @NotNull Long version,
    @NotNull LodgingKind kind,
    @NotBlank @Size(max = 160) String name,
    @Size(max = 300) String address,
    @NotNull LocalDate checkIn,
    @NotNull LocalDate checkOut,
    BoardType boardType,
    @Min(1) @Max(4) Integer priceTier,
    @Size(max = 80) String bookingRef,
    @Size(max = 2000) String notes) {

  LodgingDetails toDomain() {
    return new LodgingRequest(
            kind, name, address, checkIn, checkOut, boardType, priceTier, bookingRef, notes, null)
        .toDomain();
  }
}
