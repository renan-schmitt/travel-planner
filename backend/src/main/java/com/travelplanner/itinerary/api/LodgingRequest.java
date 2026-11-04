package com.travelplanner.itinerary.api;

import com.travelplanner.itinerary.domain.BoardType;
import com.travelplanner.itinerary.domain.LodgingDetails;
import com.travelplanner.itinerary.domain.LodgingKind;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Nova hospedagem numa parada (hotel, aluguel ou casa de amigo/parente). {@code priceTier} de 1
 * ({@code $}) a 4 ({@code $$$$}), ou ausente = "não sei". As regras de datas ficam em {@link
 * LodgingDetails#validate}.
 *
 * <p>Com {@code placeId} (resultado da busca de hotéis), o backend busca nome e endereço no Places:
 * {@code name} e {@code address} passam a ser opcionais (se vierem, prevalecem). Sem {@code
 * placeId}, {@code name} é obrigatório.
 */
public record LodgingRequest(
    @NotNull LodgingKind kind,
    @Size(max = 160) String name,
    @Size(max = 300) String address,
    @NotNull LocalDate checkIn,
    @NotNull LocalDate checkOut,
    BoardType boardType,
    @Min(1) @Max(4) Integer priceTier,
    @Size(max = 80) String bookingRef,
    @Size(max = 2000) String notes,
    @Pattern(regexp = "[A-Za-z0-9_-]{1,255}") String placeId) {

  LodgingDetails toDomain() {
    return new LodgingDetails(
        kind,
        SegmentRequest.clean(name),
        SegmentRequest.clean(address),
        checkIn,
        checkOut,
        boardType,
        priceTier,
        SegmentRequest.clean(bookingRef),
        SegmentRequest.clean(notes));
  }
}
