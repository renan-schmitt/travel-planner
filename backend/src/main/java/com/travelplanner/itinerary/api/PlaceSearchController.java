package com.travelplanner.itinerary.api;

import com.travelplanner.auth.domain.AppUserPrincipal;
import com.travelplanner.itinerary.domain.PlaceSearchService;
import com.travelplanner.places.domain.NearbyKind;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Busca de restaurantes e atrações perto de uma parada, no Google Places. Places fora do ar ou sem
 * chave → 503.
 */
@RestController
@RequestMapping("/api/v1/trips/{tripId}/stops/{stopId}/places-search")
@RequiredArgsConstructor
class PlaceSearchController {

  private final PlaceSearchService search;

  /**
   * @param kind restaurantes ou atrações
   * @param minRating nota mínima (ex.: 4 ou 4.5)
   * @param tiers faixas de preço aceitas (1–4; vazio = todas)
   */
  @GetMapping
  PlaceSearchResponse searchPlaces(
      @AuthenticationPrincipal AppUserPrincipal principal,
      @PathVariable long tripId,
      @PathVariable long stopId,
      @RequestParam NearbyKind kind,
      @RequestParam(required = false) @DecimalMin("0") @DecimalMax("5") Double minRating,
      @RequestParam(required = false) List<@Min(1) @Max(4) Integer> tiers) {
    Set<Integer> accepted = tiers == null ? Set.of() : new TreeSet<>(tiers);
    return PlaceSearchResponse.from(
        search.search(tripId, stopId, principal.getUserId(), kind, minRating, accepted));
  }
}
