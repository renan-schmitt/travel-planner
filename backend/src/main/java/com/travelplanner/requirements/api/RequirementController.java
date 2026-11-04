package com.travelplanner.requirements.api;

import com.travelplanner.auth.domain.AppUserPrincipal;
import com.travelplanner.requirements.domain.RequirementCatalog;
import com.travelplanner.requirements.domain.TripRequirementsService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** Leitura: países da base e documentos de uma viagem. */
@RestController
@RequiredArgsConstructor
class RequirementController {

  private final TripRequirementsService trips;
  private final RequirementCatalog catalog;

  /** Destinos estrangeiros da viagem, em ordem de código do país (VIEWER). */
  @GetMapping("/api/v1/trips/{tripId}/requirements")
  List<TripRequirementsResponse> listTripRequirements(
      @AuthenticationPrincipal AppUserPrincipal principal, @PathVariable long tripId) {
    return trips.forTrip(tripId, principal.getUserId()).stream()
        .map(TripRequirementsResponse::from)
        .toList();
  }

  /** Todos os países, em ordem de nome. */
  @GetMapping("/api/v1/countries")
  List<CountryResponse> listCountries() {
    return catalog.allCountries().stream().map(CountryResponse::from).toList();
  }
}
