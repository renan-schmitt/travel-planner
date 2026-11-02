package com.travelplanner.trip.api;

import com.travelplanner.trip.domain.TripRole;
import jakarta.validation.constraints.NotNull;

/** Mudança de papel. {@code version} divergente → 409. */
public record UpdateMemberRequest(@NotNull Long version, @NotNull TripRole role) {}
