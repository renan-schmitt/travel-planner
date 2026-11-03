package com.travelplanner.checklist.api;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/** Itens criados e quantos do template foram pulados por já existirem na viagem. */
public record ApplyTemplateResponse(
    @NotNull List<ChecklistItemResponse> added, @NotNull Integer skipped) {}
