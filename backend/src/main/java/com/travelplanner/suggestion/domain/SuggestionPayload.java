package com.travelplanner.suggestion.domain;

import java.util.Map;

/**
 * O que vai em {@code payload_json}: os parâmetros da mensagem e o item de checklist sugerido (ou
 * {@code null}).
 */
public record SuggestionPayload(Map<String, Object> params, ChecklistDraft checklist) {

  public SuggestionPayload {
    params = params == null ? Map.of() : params;
  }
}
