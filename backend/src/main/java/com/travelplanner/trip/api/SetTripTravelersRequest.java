package com.travelplanner.trip.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Quem vai na viagem: substitui a lista inteira. */
public record SetTripTravelersRequest(@NotNull @Size(max = 50) List<@NotNull Long> travelerIds) {}
