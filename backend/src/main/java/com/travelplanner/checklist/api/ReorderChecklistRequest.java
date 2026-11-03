package com.travelplanner.checklist.api;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/** Nova ordem do checklist: todos os ids dos itens da viagem, sem repetição. */
public record ReorderChecklistRequest(@NotNull List<@NotNull Long> itemIds) {}
