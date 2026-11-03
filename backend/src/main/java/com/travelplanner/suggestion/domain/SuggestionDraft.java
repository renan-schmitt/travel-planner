package com.travelplanner.suggestion.domain;

import java.util.Map;
import java.util.Optional;

/**
 * O que uma regra produz para uma lacuna. {@code logicalKey} identifica a lacuna dentro da regra
 * (ex.: o id do trecho, a data da noite): a mesma lacuna gera sempre a mesma chave, e é por ela que
 * o motor reconcilia. {@code params} montam a mensagem no frontend (valores simples: texto, número,
 * booleano, data ISO).
 */
public record SuggestionDraft(
    String logicalKey,
    Severity severity,
    Map<String, Object> params,
    Optional<ChecklistDraft> autoChecklist) {

  public SuggestionDraft {
    params = Map.copyOf(params);
  }

  public static SuggestionDraft of(
      String logicalKey, Severity severity, Map<String, Object> params) {
    return new SuggestionDraft(logicalKey, severity, params, Optional.empty());
  }

  public SuggestionDraft withChecklist(ChecklistDraft checklist) {
    return new SuggestionDraft(logicalKey, severity, params, Optional.of(checklist));
  }
}
