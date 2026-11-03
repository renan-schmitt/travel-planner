package com.travelplanner.checklist.domain;

/** Item de template. {@code dueOffsetDays}: dias em relação à partida (−7 = uma semana antes). */
public record TemplateItemDetails(
    ChecklistCategory category, String title, Integer dueOffsetDays) {}
