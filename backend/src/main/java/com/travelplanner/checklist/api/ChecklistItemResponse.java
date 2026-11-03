package com.travelplanner.checklist.api;

import com.travelplanner.checklist.domain.ChecklistCategory;
import com.travelplanner.checklist.domain.ChecklistItem;
import com.travelplanner.checklist.domain.ChecklistSource;
import com.travelplanner.checklist.domain.ChecklistStatus;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Item de checklist. {@code assigneeUserId} e {@code doneByUserId} são ids de usuário; o nome e a
 * foto vêm da lista de membros da viagem.
 */
public record ChecklistItemResponse(
    @NotNull Long id,
    @NotNull ChecklistCategory category,
    @NotNull String title,
    String notes,
    LocalDate dueDate,
    @NotNull ChecklistStatus status,
    Instant doneAt,
    Long doneByUserId,
    Long assigneeUserId,
    @NotNull ChecklistSource source,
    String ruleCode,
    @NotNull Integer sortOrder,
    @NotNull Long version) {

  static ChecklistItemResponse from(ChecklistItem item) {
    return new ChecklistItemResponse(
        item.getId(),
        item.getCategory(),
        item.getTitle(),
        item.getNotes(),
        item.getDueDate(),
        item.getStatus(),
        item.getDoneAt(),
        item.getDoneBy(),
        item.getAssigneeUserId(),
        item.getSource(),
        item.getRuleCode(),
        item.getSortOrder(),
        item.getVersion());
  }
}
