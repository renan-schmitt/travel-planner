package com.travelplanner.checklist.domain;

/**
 * Filtros da listagem (nulos = sem filtro). {@code onlyMine}: itens de que quem pede é responsável.
 */
public record ChecklistFilter(
    ChecklistCategory category, ChecklistStatus status, boolean onlyMine) {

  public static final ChecklistFilter NONE = new ChecklistFilter(null, null, false);

  boolean matches(ChecklistItem item, long userId) {
    return (category == null || item.getCategory() == category)
        && (status == null || item.getStatus() == status)
        && (!onlyMine || Long.valueOf(userId).equals(item.getAssigneeUserId()));
  }
}
