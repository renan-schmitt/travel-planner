package com.travelplanner.itinerary.api;

import com.travelplanner.itinerary.domain.PriceTierSource;
import com.travelplanner.itinerary.domain.SavedPlace;
import com.travelplanner.itinerary.domain.SavedPlaceKind;
import com.travelplanner.places.domain.PlaceInfo;
import jakarta.validation.constraints.NotNull;

/**
 * Favorito de uma parada. {@code rating}, {@code userRatingCount}, {@code lat}/{@code lng} e {@code
 * googleMapsUri} vêm do Places na listagem por parada (nulos se o Places estiver fora do ar, e
 * sempre nulos na listagem da viagem e nas respostas de criar/editar).
 */
public record SavedPlaceResponse(
    @NotNull Long id,
    @NotNull Long stopId,
    @NotNull String placeId,
    @NotNull String name,
    @NotNull SavedPlaceKind kind,
    Integer priceTier,
    PriceTierSource priceTierSource,
    String notes,
    @NotNull Boolean visited,
    Double rating,
    Integer userRatingCount,
    Double lat,
    Double lng,
    String googleMapsUri,
    @NotNull Long version) {

  static SavedPlaceResponse from(SavedPlace place, PlaceInfo info) {
    return new SavedPlaceResponse(
        place.getId(),
        place.getStopId(),
        place.getPlaceId(),
        place.getName(),
        place.getKind(),
        place.getPriceTier(),
        place.getPriceTierSource(),
        place.getNotes(),
        place.isVisited(),
        info == null ? null : info.rating(),
        info == null ? null : info.userRatingCount(),
        info == null ? null : info.lat(),
        info == null ? null : info.lng(),
        info == null ? null : info.googleMapsUri(),
        place.getVersion());
  }

  static SavedPlaceResponse from(SavedPlace place) {
    return from(place, null);
  }
}
