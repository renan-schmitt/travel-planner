package com.travelplanner.itinerary.api;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

/**
 * Nova ordem das atividades de um dia: todas as que já estão nele, mais as que vêm de outros dias.
 */
public record ReorderActivitiesRequest(
    @NotNull LocalDate day, @NotNull List<@NotNull Long> activityIds) {}
