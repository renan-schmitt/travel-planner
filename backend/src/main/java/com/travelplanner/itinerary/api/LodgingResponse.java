package com.travelplanner.itinerary.api;

import com.travelplanner.itinerary.domain.BoardType;
import com.travelplanner.itinerary.domain.Lodging;
import com.travelplanner.itinerary.domain.LodgingKind;
import com.travelplanner.itinerary.domain.PriceTierSource;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/** Hospedagem de uma parada. {@code nights} = noites entre check-in e check-out. */
public record LodgingResponse(
    @NotNull Long id,
    @NotNull Long stopId,
    @NotNull LodgingKind kind,
    String placeId,
    @NotNull String name,
    String address,
    @NotNull LocalDate checkIn,
    @NotNull LocalDate checkOut,
    @NotNull Integer nights,
    BoardType boardType,
    Integer priceTier,
    PriceTierSource priceTierSource,
    String bookingRef,
    String notes,
    @NotNull Long version) {

  static LodgingResponse from(Lodging lodging) {
    return new LodgingResponse(
        lodging.getId(),
        lodging.getStopId(),
        lodging.getKind(),
        lodging.getPlaceId(),
        lodging.getName(),
        lodging.getAddress(),
        lodging.getCheckIn(),
        lodging.getCheckOut(),
        lodging.details().nights(),
        lodging.getBoardType(),
        lodging.getPriceTier(),
        lodging.getPriceTierSource(),
        lodging.getBookingRef(),
        lodging.getNotes(),
        lodging.getVersion());
  }
}
