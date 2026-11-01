package com.travelplanner.itinerary.api;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/** Nova ordem do roteiro: todos os ids das paradas da viagem, sem repetição. */
public record ReorderStopsRequest(@NotNull List<@NotNull Long> stopIds) {}
