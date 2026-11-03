package com.travelplanner.suggestion.domain.rules;

import com.travelplanner.checklist.domain.ChecklistCategory;
import com.travelplanner.itinerary.domain.TripSnapshot.TravelerEntry;
import com.travelplanner.suggestion.domain.ChecklistDraft;
import com.travelplanner.suggestion.domain.GapContext;
import com.travelplanner.suggestion.domain.GapRule;
import com.travelplanner.suggestion.domain.Severity;
import com.travelplanner.suggestion.domain.SuggestionDraft;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * {@code PASSAPORTE_VALIDADE}: em viagem internacional, para cada viajante, passaporte sem validade
 * informada (INFO) ou vencendo antes de 6 meses depois da volta (CRITICAL, com o item "Renovar
 * passaporte de &lt;nome&gt;"). Brasileiros indo só a países que aceitam o RG ficam de fora (lista
 * fixa; será substituída pela base de requisitos na Fase 19).
 */
@Component
public class PassportValidityRule implements GapRule {

  public static final String CODE = "PASSAPORTE_VALIDADE";
  static final Set<String> BRAZILIAN_ID_COUNTRIES =
      Set.of("AR", "UY", "PY", "CL", "BO", "PE", "CO", "EC", "VE");
  static final int MONTHS_AFTER_RETURN = 6;

  @Override
  public String code() {
    return CODE;
  }

  @Override
  public List<SuggestionDraft> evaluate(GapContext context) {
    Set<String> countries = context.foreignCountries();
    if (countries.isEmpty()) {
      return List.of();
    }
    boolean idCardEnough = BRAZILIAN_ID_COUNTRIES.containsAll(countries);
    LocalDate returnDate =
        context.snapshot().trip().endDate() != null
            ? context.snapshot().trip().endDate()
            : context.snapshot().trip().startDate();
    List<SuggestionDraft> drafts = new ArrayList<>();
    for (TravelerEntry traveler : context.snapshot().travelers()) {
      var d = traveler.details();
      if (idCardEnough && d.nationalities().contains("BR")) {
        continue;
      }
      Map<String, Object> params = new LinkedHashMap<>();
      params.put("travelerId", traveler.id());
      params.put("name", d.name());
      String key = String.valueOf(traveler.id());
      if (d.passportExpiry() == null) {
        params.put("missing", true);
        drafts.add(SuggestionDraft.of(key, Severity.INFO, params));
        continue;
      }
      if (returnDate == null) {
        continue;
      }
      LocalDate limit = returnDate.plusMonths(MONTHS_AFTER_RETURN);
      if (d.passportExpiry().isBefore(limit)) {
        params.put("missing", false);
        params.put("expiry", d.passportExpiry().toString());
        params.put("limit", limit.toString());
        drafts.add(
            SuggestionDraft.of(key, Severity.CRITICAL, params)
                .withChecklist(
                    new ChecklistDraft(
                        ChecklistCategory.DOCUMENTS,
                        "Renovar passaporte de " + d.name(),
                        null,
                        true)));
      }
    }
    return drafts;
  }
}
