package com.travelplanner.requirements.domain;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Países e requisitos publicados de um conjunto de países (entrada das regras de documentos). */
public record RequirementsData(
    Map<String, CountryInfo> countries, List<PublishedRequirement> published) {

  public RequirementsData {
    countries = Map.copyOf(countries);
    published = List.copyOf(published);
  }

  public static RequirementsData empty() {
    return new RequirementsData(Map.of(), List.of());
  }

  public Optional<CountryInfo> country(String code) {
    return Optional.ofNullable(code == null ? null : countries.get(code));
  }

  public List<PublishedRequirement> forCountry(String code, String nationality) {
    return published.stream()
        .filter(r -> r.countryCode().equals(code) && r.nationality().equals(nationality))
        .toList();
  }
}
