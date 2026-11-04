package com.travelplanner.itinerary.api;

import com.travelplanner.itinerary.domain.PlaceSearchService;
import com.travelplanner.itinerary.domain.PriceTierSource;
import com.travelplanner.places.domain.PlaceResult;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Restaurantes ou atrações perto da parada (até 20, por popularidade) e o centro da busca.
 *
 * @param center {@code LODGING} (a hospedagem {@code centerLodgingName}), {@code CITY} ou {@code
 *     TEXT} (parada sem coordenadas: busca por texto na cidade)
 */
public record PlaceSearchResponse(
    @NotNull List<PlaceSearchResult> results,
    @NotNull PlaceSearchService.CenterKind center,
    Double centerLat,
    Double centerLng,
    String centerLodgingName) {

  /**
   * Lugar encontrado no Places.
   *
   * @param priceTier 1–4, nula se não houver como estimar
   * @param priceTierSource {@code PLACES} ou {@code HEURISTIC} ("estimado")
   * @param photoName para {@code GET /api/v1/places/photos?name=}
   */
  public record PlaceSearchResult(
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
      String googleMapsUri) {

    static PlaceSearchResult from(PlaceResult place) {
      var tier = place.priceTier();
      return new PlaceSearchResult(
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
          place.googleMapsUri());
    }
  }

  static PlaceSearchResponse from(PlaceSearchService.Result result) {
    var center = result.center();
    return new PlaceSearchResponse(
        result.places().stream().map(PlaceSearchResult::from).toList(),
        center.kind(),
        center.lat(),
        center.lng(),
        center.lodgingName());
  }
}
