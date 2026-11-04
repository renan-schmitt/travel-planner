package com.travelplanner.suggestion.domain.rules;

import com.travelplanner.requirements.domain.PublishedRequirement;
import com.travelplanner.requirements.domain.TripRequirementsService.ApplicableRequirement;
import com.travelplanner.requirements.domain.TripRequirementsService.DestinationView;
import com.travelplanner.suggestion.domain.GapContext;
import com.travelplanner.suggestion.domain.GapRule;
import com.travelplanner.suggestion.domain.Severity;
import com.travelplanner.suggestion.domain.SuggestionDraft;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * {@code REQUISITO_DESATUALIZADO}: requisito usado na viagem verificado há mais de 6 meses (ou sem
 * data de verificação). INFO, sem checklist; só administradores veem (ver {@code
 * SuggestionService}).
 */
@Component
public class OutdatedRequirementRule implements GapRule {

  public static final String CODE = "REQUISITO_DESATUALIZADO";
  static final int MAX_AGE_MONTHS = 6;

  @Override
  public String code() {
    return CODE;
  }

  @Override
  public List<SuggestionDraft> evaluate(GapContext context) {
    LocalDate limit =
        LocalDate.ofInstant(context.now(), ZoneOffset.UTC).minusMonths(MAX_AGE_MONTHS);
    List<SuggestionDraft> drafts = new ArrayList<>();
    for (DestinationView destination : DocumentRuleSupport.destinations(context)) {
      for (ApplicableRequirement applicable : destination.requirements()) {
        PublishedRequirement r = applicable.requirement();
        if (r.verifiedAt() != null && !r.verifiedAt().isBefore(limit)) {
          continue;
        }
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("requirementId", r.id());
        DocumentRuleSupport.putCountry(params, destination);
        params.put("title", r.title());
        RuleSupport.putIfPresent(
            params, "verifiedAt", r.verifiedAt() == null ? null : r.verifiedAt().toString());
        drafts.add(SuggestionDraft.of(String.valueOf(r.id()), Severity.INFO, params));
      }
    }
    return drafts;
  }
}
