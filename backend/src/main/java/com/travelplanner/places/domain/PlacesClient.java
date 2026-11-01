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
}
