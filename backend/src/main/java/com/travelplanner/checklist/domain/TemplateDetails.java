package com.travelplanner.checklist.domain;

import java.util.List;

/** Campos editáveis de um template do usuário; os itens ficam na ordem da lista. */
public record TemplateDetails(
    String name, String description, TemplateIcon icon, List<TemplateItemDetails> items) {}
