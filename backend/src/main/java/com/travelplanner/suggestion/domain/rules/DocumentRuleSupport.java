package com.travelplanner.suggestion.domain.rules;

import com.travelplanner.requirements.domain.CountryInfo;
import com.travelplanner.requirements.domain.TripDestinations;
import com.travelplanner.requirements.domain.TripRequirementsService;
import com.travelplanner.requirements.domain.TripRequirementsService.DestinationView;
import com.travelplanner.suggestion.domain.GapContext;
import java.util.List;
import java.util.Map;

/** Utilidades das regras de documentos. */
final class DocumentRuleSupport {

  private DocumentRuleSupport() {}

  /** Destinos estrangeiros cruzados com a base (mesmo cálculo da tela de documentos). */
  static List<DestinationView> destinations(GapContext context) {
    return TripDestinations.of(context.snapshot()).stream()
        .map(d -> TripRequirementsService.view(d, context.requirements()))
        .toList();
  }

  /** Nome do país em pt-BR (o código, se o país não está na base). */
  static String countryName(DestinationView destination) {
    CountryInfo country = destination.country();
    return country != null ? country.namePt() : destination.countryCode();
  }

  static void putCountry(Map<String, Object> params, DestinationView destination) {
    params.put("countryCode", destination.countryCode());
    params.put("country", countryName(destination));
  }
}
