package com.travelplanner.checklist.api;

import com.travelplanner.checklist.domain.TemplateDetails;
import com.travelplanner.checklist.domain.TemplateIcon;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Edição de um template do usuário (substitui tudo, inclusive os itens). {@code version} → 409. */
public record UpdateTemplateRequest(
    @NotNull Long version,
    @NotBlank @Size(max = 120) String name,
    @Size(max = 500) String description,
    @NotNull TemplateIcon icon,
    @NotNull @Size(max = 100) List<@Valid @NotNull TemplateItemPayload> items) {

  TemplateDetails toDomain() {
    return new TemplateRequest(name, description, icon, items).toDomain();
  }
}
