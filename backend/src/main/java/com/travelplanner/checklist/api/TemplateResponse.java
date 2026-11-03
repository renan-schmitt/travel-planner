package com.travelplanner.checklist.api;

import com.travelplanner.checklist.domain.ChecklistTemplate;
import com.travelplanner.checklist.domain.TemplateIcon;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** Template com os itens. {@code system}: do sistema (ninguém edita). */
public record TemplateResponse(
    @NotNull Long id,
    @NotNull String name,
    String description,
    @NotNull TemplateIcon icon,
    @NotNull Boolean system,
    @NotNull List<TemplateItemPayload> items,
    @NotNull Long version) {

  static TemplateResponse from(ChecklistTemplate template) {
    return new TemplateResponse(
        template.getId(),
        template.getName(),
        template.getDescription(),
        template.getIcon(),
        template.isSystem(),
        template.getItems().stream().map(TemplateItemPayload::from).toList(),
        template.getVersion());
  }
}
