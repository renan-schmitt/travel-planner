package com.travelplanner.itinerary.api;

import com.travelplanner.auth.domain.AppUserPrincipal;
import com.travelplanner.itinerary.domain.LodgingSearchService;
import com.travelplanner.places.domain.LodgingType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
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
 * Busca de hotéis/resorts perto de uma parada, no Google Places. Places fora do ar ou sem chave →
 * 503.
 */
@RestController
@RequestMapping("/api/v1/trips/{tripId}/stops/{stopId}/lodging-search")
@RequiredArgsConstructor
class LodgingSearchController {

  private final LodgingSearchService search;

  /**
   * @param type hotel, resort ou qualquer hospedagem
   * @param minRating nota mínima (ex.: 4 ou 4.5)
   * @param tiers faixas de preço aceitas (1–4; vazio = todas)
   * @param page {@code nextPageToken} da página anterior
   */
  @GetMapping
  LodgingSearchResponse searchLodging(
      @AuthenticationPrincipal AppUserPrincipal principal,
      @PathVariable long tripId,
      @PathVariable long stopId,
      @RequestParam(defaultValue = "HOTEL") LodgingType type,
      @RequestParam(required = false) @DecimalMin("0") @DecimalMax("5") Double minRating,
      @RequestParam(required = false) List<@Min(1) @Max(4) Integer> tiers,
      @RequestParam(required = false) @Size(max = 2048) String page) {
    Set<Integer> accepted = tiers == null ? Set.of() : new TreeSet<>(tiers);
    String pageToken = page == null || page.isBlank() ? null : page;
    return LodgingSearchResponse.from(
        search.search(tripId, stopId, principal.getUserId(), type, minRating, accepted, pageToken));
  }
}
