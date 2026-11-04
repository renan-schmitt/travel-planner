package com.travelplanner.itinerary.api;

import com.travelplanner.itinerary.domain.PriceTierSource;
import com.travelplanner.places.domain.LodgingPage;
import com.travelplanner.places.domain.LodgingPlace;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** Uma página da busca de hospedagem; {@code nextPageToken} nulo = última página. */
public record LodgingSearchResponse(
    @NotNull List<LodgingSearchResult> results, String nextPageToken) {

  /**
   * Hospedagem encontrada no Places.
   *
   * @param priceTier 1–4, nula se não houver como estimar
   * @param priceTierSource {@code PLACES} ou {@code HEURISTIC} ("estimado")
   * @param photoName para {@code GET /api/v1/places/photos?name=}
   */
  public record LodgingSearchResult(
      @NotNull String placeId,
      @NotNull String name,
      String address,
      Double lat,
      Double lng,
      Double rating,
      Integer userRatingCount,
      Integer priceTier,
      PriceTierSource priceTierSource,
      String primaryType,
      String photoName,
      String websiteUri,
      String googleMapsUri) {

    static LodgingSearchResult from(LodgingPlace place) {
      var tier = place.priceTier();
      return new LodgingSearchResult(
          place.placeId(),
          place.name() != null ? place.name() : place.placeId(),
          place.address(),
          place.lat(),
          place.lng(),
          place.rating(),
          place.userRatingCount(),
          tier == null ? null : tier.tier(),
          tier == null ? null : PriceTierSource.valueOf(tier.source().name()),
          place.primaryType(),
          place.photoName(),
          place.websiteUri(),
          place.googleMapsUri());
    }
  }

  static LodgingSearchResponse from(LodgingPage page) {
    return new LodgingSearchResponse(
        page.places().stream().map(LodgingSearchResult::from).toList(), page.nextPageToken());
  }
}
