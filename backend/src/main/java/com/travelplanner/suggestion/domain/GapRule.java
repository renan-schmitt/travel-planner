package com.travelplanner.suggestion.domain;

import java.util.List;

/**
 * Regra de lacuna. Implementações são beans Spring ({@code @Component}), descobertas
 * automaticamente pelo {@link SuggestionEngine}, e <b>puras</b>: só leem o {@link GapContext}, sem
 * banco nem rede. Uma regra nova entra sem mexer nas existentes.
 */
public interface GapRule {

  /** Código estável da regra (ex.: {@code TRANSFER_CHEGADA}); chave do i18n no frontend. */
  String code();

  List<SuggestionDraft> evaluate(GapContext context);
}
