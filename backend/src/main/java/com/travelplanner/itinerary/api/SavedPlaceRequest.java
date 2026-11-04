package com.travelplanner.itinerary.api;

import com.travelplanner.itinerary.domain.SavedPlaceKind;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Novo favorito, a partir de um resultado da busca. {@code name} é opcional (sem ele, vem do
 * Places); {@code priceTier} igual à da busca guarda a origem dela ({@code PLACES}/{@code
 * HEURISTIC}). O mesmo lugar duas vezes na parada → 409.
 */
public record SavedPlaceRequest(
    @NotNull @Pattern(regexp = "[A-Za-z0-9_-]{1,255}") String placeId,
    @NotNull SavedPlaceKind kind,
    @Size(max = 160) String name,
    @Min(1) @Max(4) Integer priceTier,
    @Size(max = 2000) String notes) {}
