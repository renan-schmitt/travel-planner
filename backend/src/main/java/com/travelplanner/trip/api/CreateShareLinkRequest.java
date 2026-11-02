package com.travelplanner.trip.api;

import com.travelplanner.trip.domain.TripRole;
import jakarta.validation.constraints.NotNull;

/** Novo link de partilha: {@code EDITOR} ou {@code VIEWER}. */
public record CreateShareLinkRequest(@NotNull TripRole role) {}
