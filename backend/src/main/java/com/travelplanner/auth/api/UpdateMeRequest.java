package com.travelplanner.auth.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Edição do perfil. Substitui nacionalidade e cidade de origem; {@code homeCity} nulo limpa a
 * cidade. {@code version} é a lida no {@code GET} (divergente → 409).
 */
public record UpdateMeRequest(
    @NotNull Long version,
    @NotNull @Pattern(regexp = "[A-Z]{2}") String nationality,
    @Valid HomeCityDto homeCity) {}
