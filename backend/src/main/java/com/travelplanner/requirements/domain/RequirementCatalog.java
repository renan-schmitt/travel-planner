package com.travelplanner.requirements.domain;

import com.travelplanner.requirements.infra.CountryRepository;
import com.travelplanner.requirements.infra.DestinationRequirementRepository;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Leitura da base: países e requisitos publicados (sem verificação de acesso). */
@Service
@RequiredArgsConstructor
public class RequirementCatalog {

  private final CountryRepository countries;
  private final DestinationRequirementRepository requirements;

  @Transactional(readOnly = true)
  public RequirementsData load(Collection<String> countryCodes) {
    List<String> codes =
        countryCodes.stream().map(c -> c.toUpperCase(Locale.ROOT)).distinct().toList();
    if (codes.isEmpty()) {
      return RequirementsData.empty();
    }
    return new RequirementsData(
        countries.findAllById(codes).stream()
            .map(Country::toInfo)
            .collect(Collectors.toMap(CountryInfo::code, Function.identity())),
        requirements.findByCountryCodeInAndStatus(codes, RequirementStatus.PUBLISHED).stream()
            .map(DestinationRequirement::toPublished)
            .toList());
  }

  @Transactional(readOnly = true)
  public List<CountryInfo> allCountries() {
    return countries.findAllByOrderByNamePt().stream().map(Country::toInfo).toList();
  }

  public boolean countryExists(String code) {
    return code != null && countries.existsById(code.toUpperCase(Locale.ROOT));
  }
}
