package com.travelplanner.checklist.api;

import com.travelplanner.checklist.domain.ChecklistStatus;
import jakarta.validation.constraints.NotNull;

public record ChecklistStatusRequest(@NotNull ChecklistStatus status) {}
