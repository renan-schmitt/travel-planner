package com.travelplanner.trip.api;

import com.travelplanner.trip.domain.TripRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Convite por e-mail com o papel na viagem. */
public record InviteMemberRequest(
    @NotBlank @Email @Size(max = 320) String email, @NotNull TripRole role) {}
