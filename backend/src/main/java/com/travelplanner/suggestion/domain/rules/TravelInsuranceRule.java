package com.travelplanner.suggestion.domain.rules;

import com.travelplanner.checklist.domain.ChecklistCategory;
import com.travelplanner.suggestion.domain.ChecklistDraft;
import com.travelplanner.suggestion.domain.GapContext;
import com.travelplanner.suggestion.domain.GapRule;
import com.travelplanner.suggestion.domain.Severity;
import com.travelplanner.suggestion.domain.SuggestionDraft;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * {@code SEGURO_VIAGEM}: viagem internacional sem item de seguro no checklist (aberto ou
 * concluído): o item criado por esta regra, ou qualquer item cujo título fale em "seguro viagem"
 * (ex.: o do template Internacional). WARNING se algum país for do espaço Schengen (seguro
 * obrigatório), senão INFO. Aceitar cria "Contratar seguro viagem", 14 dias antes da partida.
 */
@Component
public class TravelInsuranceRule implements GapRule {

  public static final String CODE = "SEGURO_VIAGEM";
  static final String KEY = "trip";
  static final int DAYS_BEFORE = 14;

  /** Espaço Schengen (lista fixa por enquanto; a base de requisitos chega na Fase 19). */
  static final Set<String> SCHENGEN =
      Set.of(
          "AT", "BE", "BG", "CH", "CZ", "DE", "DK", "EE", "ES", "FI", "FR", "GR", "HR", "HU", "IS",
          "IT", "LI", "LT", "LU", "LV", "MT", "NL", "NO", "PL", "PT", "RO", "SE", "SI", "SK");

  @Override
  public String code() {
    return CODE;
  }

  @Override
  public List<SuggestionDraft> evaluate(GapContext context) {
    Set<String> countries = context.foreignCountries();
    if (countries.isEmpty() || hasInsuranceItem(context)) {
      return List.of();
    }
    boolean schengen = countries.stream().anyMatch(SCHENGEN::contains);
    LocalDate start = context.snapshot().trip().startDate();
    return List.of(
        SuggestionDraft.of(
                KEY, schengen ? Severity.WARNING : Severity.INFO, Map.of("schengen", schengen))
            .withChecklist(
                new ChecklistDraft(
                    ChecklistCategory.DOCUMENTS,
                    "Contratar seguro viagem",
                    start == null ? null : start.minusDays(DAYS_BEFORE),
                    false)));
  }

  private static boolean hasInsuranceItem(GapContext context) {
    return context.checklist().stream()
        .anyMatch(
            item -> {
              if (CODE.equals(item.ruleCode())) {
                return true;
              }
              String title = GapContext.normalize(item.title());
              return title.contains("seguro viagem") || title.contains("seguro de viagem");
            });
  }
}
