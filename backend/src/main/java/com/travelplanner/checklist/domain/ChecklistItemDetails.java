package com.travelplanner.checklist.domain;

import java.time.LocalDate;

/** Campos editáveis do item. {@code assigneeUserId} precisa ser de um membro com conta. */
public record ChecklistItemDetails(
    ChecklistCategory category,
    String title,
    String notes,
    LocalDate dueDate,
    Long assigneeUserId) {}
