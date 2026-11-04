package com.travelplanner.itinerary.api;

import com.travelplanner.itinerary.domain.SavedPlaceKind;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Edição do favorito: substitui tipo, faixa, notas e "visitado" (lugar e nome não mudam). {@code
 * version} divergente → 409.
 */
public record UpdateSavedPlaceRequest(
    @NotNull Long version,
    @NotNull SavedPlaceKind kind,
    @Min(1) @Max(4) Integer priceTier,
    @Size(max = 2000) String notes,
    @NotNull Boolean visited) {}
