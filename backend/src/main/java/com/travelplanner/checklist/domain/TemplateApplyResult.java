package com.travelplanner.checklist.domain;

import java.util.List;

/** Itens criados ao aplicar um template e quantos foram pulados por já existirem na viagem. */
public record TemplateApplyResult(List<ChecklistItem> added, int skipped) {}
