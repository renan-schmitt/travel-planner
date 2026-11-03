package com.travelplanner.checklist.api;

import com.travelplanner.checklist.domain.ChecklistCategory;
import com.travelplanner.checklist.domain.ChecklistItemDetails;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Edição do item (substitui os campos editáveis; não muda o status). {@code version} → 409. */
public record UpdateChecklistItemRequest(
    @NotNull Long version,
    @NotNull ChecklistCategory category,
    @NotBlank @Size(max = 200) String title,
    @Size(max = 2000) String notes,
    LocalDate dueDate,
    Long assigneeUserId) {

  ChecklistItemDetails toDomain() {
    return new ChecklistItemRequest(category, title, notes, dueDate, assigneeUserId).toDomain();
  }
}
