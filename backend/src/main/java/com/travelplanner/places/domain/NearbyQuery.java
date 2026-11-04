package com.travelplanner.places.domain;

/**
 * Busca de restaurantes/atrações perto de um ponto. Também é a chave do cache (record: igualdade
 * por valor).
 *
 * @param lat centro (a hospedagem da parada ou o centro da cidade); nulo em parada digitada à mão:
 *     busca por texto ("restaurants in Lisboa, Portugal")
 * @param city nome da cidade (para a busca por texto)
 * @param countryCode ISO-2 da cidade
 */
public record NearbyQuery(
    NearbyKind kind, Double lat, Double lng, String city, String countryCode) {

  public boolean hasCenter() {
    return lat != null && lng != null;
  }
}
