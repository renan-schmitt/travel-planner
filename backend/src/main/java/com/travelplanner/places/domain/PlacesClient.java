package com.travelplanner.places.domain;

import java.util.List;

/**
 * Integração com o Google Places (API New). Implementações lançam {@link
 * com.travelplanner.shared.web.ServiceUnavailableException} em falha, timeout ou quota esgotada.
 */
public interface PlacesClient {

  /** Cidades que começam com {@code input}. {@code sessionToken} agrupa a cobrança da sessão. */
  List<CitySuggestion> autocompleteCities(String input, String sessionToken, String languageCode);

  /**
   * Detalhes da cidade (field mask mínima). O {@code sessionToken} do autocomplete encerra a
   * sessão.
   *
   * @throws com.travelplanner.shared.web.NotFoundException se o {@code placeId} não existir
   */
  PlaceCity getCity(String placeId, String sessionToken, String languageCode);

  /**
   * Hospedagens perto da cidade ({@code POST /places:searchText}, {@code includedType: lodging}).
   * Os campos pedidos caem no SKU Enterprise: a quota diária é ajustada no console.
   */
  LodgingPage searchLodging(LodgingQuery query, String languageCode);

  /**
   * Nome e endereço de um lugar (field mask mínima).
   *
   * @throws com.travelplanner.shared.web.NotFoundException se o {@code placeId} não existir
   */
  PlaceSummary getPlaceSummary(String placeId, String languageCode);

  /**
   * URL pública (sem a chave) de uma foto do Places, com a largura máxima pedida.
   *
   * @param photoName {@code places/<placeId>/photos/<photoId>}
   * @throws com.travelplanner.shared.web.NotFoundException se a foto não existir
   */
  String photoUri(String photoName, int maxWidthPx);
}
