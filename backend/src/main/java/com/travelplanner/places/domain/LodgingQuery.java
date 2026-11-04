package com.travelplanner.places.domain;

/**
 * Busca de hospedagem perto de uma cidade. Também é a chave do cache (record: igualdade por valor).
 *
 * @param city nome da cidade
 * @param countryCode ISO-2 da cidade
 * @param lat latitude do centro (nula em parada digitada à mão: sem {@code locationBias})
 * @param minRating nota mínima (nula = qualquer)
 * @param pageToken {@code nextPageToken} da página anterior (nulo = primeira página)
 */
public record LodgingQuery(
    String city,
    String countryCode,
    Double lat,
    Double lng,
    LodgingType type,
    Double minRating,
    String pageToken) {}
