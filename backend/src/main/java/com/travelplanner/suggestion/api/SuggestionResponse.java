package com.travelplanner.suggestion.api;

import com.travelplanner.checklist.domain.ChecklistCategory;
import com.travelplanner.suggestion.domain.ChecklistDraft;
import com.travelplanner.suggestion.domain.Severity;
import com.travelplanner.suggestion.domain.SuggestionService.SuggestionView;
import com.travelplanner.suggestion.domain.SuggestionStatus;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.Map;

/**
 * Sugestão do motor de lacunas. O texto não vem do backend: o frontend monta título e descrição
 * pelo i18n ({@code suggestions.<ruleCode>.*}) com os {@code params}. {@code status} é o efetivo de
 * hoje (adiada vencida = {@code PENDING}). {@code checklist}: o item que a sugestão cria (na hora,
 * se {@code automatic}; senão ao aceitar).
 */
public record SuggestionResponse(
    @NotNull Long id,
    @NotNull String ruleCode,
    @NotNull String logicalKey,
    @NotNull Severity severity,
    @NotNull SuggestionStatus status,
    LocalDate snoozedUntil,
    @NotNull Map<String, Object> params,
    ChecklistSuggestion checklist) {

  public record ChecklistSuggestion(
      @NotNull ChecklistCategory category,
      @NotNull String title,
      LocalDate dueDate,
      @NotNull Boolean automatic) {

    static ChecklistSuggestion from(ChecklistDraft draft) {
      return draft == null
          ? null
          : new ChecklistSuggestion(
              draft.category(), draft.title(), draft.dueDate(), draft.automatic());
    }
  }

  static SuggestionResponse from(SuggestionView view) {
    var s = view.suggestion();
    return new SuggestionResponse(
        s.getId(),
        s.getRuleCode(),
        s.getLogicalKey(),
        s.getSeverity(),
        view.status(),
        view.status() == SuggestionStatus.SNOOZED ? s.getSnoozedUntil() : null,
        view.payload().params(),
        ChecklistSuggestion.from(view.payload().checklist()));
  }
}
