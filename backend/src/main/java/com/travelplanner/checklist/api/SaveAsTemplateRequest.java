package com.travelplanner.checklist.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SaveAsTemplateRequest(@NotBlank @Size(max = 120) String name) {}
