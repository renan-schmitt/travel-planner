package com.travelplanner.suggestion.domain.rules;

import com.travelplanner.checklist.domain.ChecklistCategory;
import com.travelplanner.requirements.domain.PublishedRequirement;
import com.travelplanner.requirements.domain.RequirementType;
import com.travelplanner.requirements.domain.TripRequirementsService.ApplicableRequirement;
import com.travelplanner.requirements.domain.TripRequirementsService.DestinationView;
import com.travelplanner.suggestion.domain.ChecklistDraft;
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
 * {@code DOCUMENTO_DESTINO}: um requisito publicado do país × nacionalidade de algum viajante.
 * WARNING (CRITICAL para visto), com o item de checklist automático: título do requisito (com o
 * país, se o título não o cita), prazo = chegada ao país − {@code leadDays} (visto sem {@code
 * leadDays}: − 30 dias) e as notas com a descrição e o link oficial.
 */
@Component
public class DestinationDocumentRule implements GapRule {

  public static final String CODE = "DOCUMENTO_DESTINO";

  @Override
  public String code() {
    return CODE;
  }

  @Override
  public List<SuggestionDraft> evaluate(GapContext context) {
    List<SuggestionDraft> drafts = new ArrayList<>();
    for (DestinationView destination : DocumentRuleSupport.destinations(context)) {
      for (ApplicableRequirement applicable : destination.requirements()) {
        drafts.add(draft(destination, applicable));
      }
    }
    return drafts;
  }

  private static SuggestionDraft draft(DestinationView destination, ApplicableRequirement a) {
    PublishedRequirement r = a.requirement();
    String country = DocumentRuleSupport.countryName(destination);
    Map<String, Object> params = new LinkedHashMap<>();
    params.put("requirementId", r.id());
    DocumentRuleSupport.putCountry(params, destination);
    params.put("nationality", r.nationality());
    params.put("type", r.type().name());
    params.put("title", r.title());
    params.put("officialUrl", r.officialUrl());
    params.put("arrivalDate", destination.arrival().toString());
    RuleSupport.putIfPresent(
        params, "dueDate", a.dueDate() == null ? null : a.dueDate().toString());
    if (!a.travelers().isEmpty()) {
      params.put("travelers", String.join(", ", a.travelers()));
    }
    String title =
        GapContext.normalize(r.title()).contains(GapContext.normalize(country))
            ? r.title()
            : r.title() + " (" + country + ")";
    String notes =
        (r.description() == null ? "" : r.description() + "\n\n")
            + "Fonte oficial: "
            + r.officialUrl();
    return SuggestionDraft.of(
            String.valueOf(r.id()),
            r.type() == RequirementType.VISA ? Severity.CRITICAL : Severity.WARNING,
            params)
        .withChecklist(
            new ChecklistDraft(ChecklistCategory.DOCUMENTS, title, a.dueDate(), true, notes));
  }
}
