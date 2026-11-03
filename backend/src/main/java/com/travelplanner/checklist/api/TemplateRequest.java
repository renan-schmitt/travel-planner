package com.travelplanner.checklist.api;

import com.travelplanner.checklist.domain.TemplateDetails;
import com.travelplanner.checklist.domain.TemplateIcon;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Novo template do usuário. Os itens ficam na ordem da lista. */
public record TemplateRequest(
    @NotBlank @Size(max = 120) String name,
    @Size(max = 500) String description,
    @NotNull TemplateIcon icon,
    @NotNull @Size(max = 100) List<@Valid @NotNull TemplateItemPayload> items) {

  TemplateDetails toDomain() {
    return new TemplateDetails(
        name.strip(),
        description == null || description.isBlank() ? null : description.strip(),
        icon,
        items.stream().map(TemplateItemPayload::toDomain).toList());
  }
}
