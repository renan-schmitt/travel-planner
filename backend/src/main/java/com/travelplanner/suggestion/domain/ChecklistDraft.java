package com.travelplanner.suggestion.domain;

import com.travelplanner.checklist.domain.ChecklistCategory;
import java.time.LocalDate;

/**
 * Item de checklist que acompanha a sugestão. {@code automatic}: criado assim que a regra produz a
 * sugestão; senão, só quando o usuário aceita. O título vai pronto (pt-BR): o item é um dado do
 * usuário, não uma mensagem do sistema.
 */
public record ChecklistDraft(
    ChecklistCategory category, String title, LocalDate dueDate, boolean automatic) {}
