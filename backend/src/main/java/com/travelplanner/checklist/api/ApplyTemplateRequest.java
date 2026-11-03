package com.travelplanner.checklist.api;

import jakarta.validation.constraints.NotNull;

public record ApplyTemplateRequest(@NotNull Long templateId) {}
