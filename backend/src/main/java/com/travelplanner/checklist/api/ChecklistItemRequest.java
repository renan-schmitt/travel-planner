package com.travelplanner.checklist.api;

import com.travelplanner.checklist.domain.ChecklistCategory;
import com.travelplanner.checklist.domain.ChecklistItemDetails;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Novo item manual. {@code assigneeUserId}: id de um usuário que participa da viagem. */
public record ChecklistItemRequest(
    @NotNull ChecklistCategory category,
    @NotBlank @Size(max = 200) String title,
    @Size(max = 2000) String notes,
    LocalDate dueDate,
    Long assigneeUserId) {

  ChecklistItemDetails toDomain() {
    return new ChecklistItemDetails(
        category,
        title.strip(),
        notes == null || notes.isBlank() ? null : notes.strip(),
        dueDate,
        assigneeUserId);
  }
}
