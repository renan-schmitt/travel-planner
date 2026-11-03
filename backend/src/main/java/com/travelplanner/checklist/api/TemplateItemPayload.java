package com.travelplanner.checklist.api;

import com.travelplanner.checklist.domain.ChecklistCategory;
import com.travelplanner.checklist.domain.TemplateItem;
import com.travelplanner.checklist.domain.TemplateItemDetails;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Item de template (entrada e saída). {@code dueOffsetDays}: dias em relação à partida (−7 = uma
 * semana antes); nulo = sem data limite.
 */
public record TemplateItemPayload(
    @NotNull ChecklistCategory category,
    @NotBlank @Size(max = 200) String title,
    @Min(-365) @Max(365) Integer dueOffsetDays) {

  static TemplateItemPayload from(TemplateItem item) {
    return new TemplateItemPayload(item.getCategory(), item.getTitle(), item.getDueOffsetDays());
  }

  TemplateItemDetails toDomain() {
    return new TemplateItemDetails(category, title.strip(), dueOffsetDays);
  }
}
