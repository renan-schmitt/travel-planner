package com.travelplanner.suggestion.domain.rules;

import com.travelplanner.requirements.domain.TripRequirementsService.DestinationView;
import com.travelplanner.suggestion.domain.GapContext;
import com.travelplanner.suggestion.domain.GapRule;
import com.travelplanner.suggestion.domain.Severity;
import com.travelplanner.suggestion.domain.SuggestionDraft;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * {@code DESTINO_SEM_REQUISITOS}: país da viagem (≠ casa) sem nenhum requisito publicado para a
 * nacionalidade de algum viajante. WARNING, sem checklist: a tela mostra "sem informações de
 * documentos" (nunca um "não precisa de nada" silencioso). {@code params.nationalities}: códigos
 * separados por vírgula.
 */
@Component
public class DestinationWithoutRequirementsRule implements GapRule {

  public static final String CODE = "DESTINO_SEM_REQUISITOS";

  @Override
  public String code() {
    return CODE;
  }

  @Override
  public List<SuggestionDraft> evaluate(GapContext context) {
    List<SuggestionDraft> drafts = new ArrayList<>();
    for (DestinationView destination : DocumentRuleSupport.destinations(context)) {
      if (destination.nationalitiesWithoutData().isEmpty()) {
        continue;
      }
      Map<String, Object> params = new LinkedHashMap<>();
      DocumentRuleSupport.putCountry(params, destination);
      params.put("nationalities", String.join(",", destination.nationalitiesWithoutData()));
      drafts.add(SuggestionDraft.of(destination.countryCode(), Severity.WARNING, params));
    }
    return drafts;
  }
}
