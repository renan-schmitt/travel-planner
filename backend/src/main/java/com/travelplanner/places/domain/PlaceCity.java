package com.travelplanner.places.domain;

/**
 * Cidade como o Places devolve (sem o fuso, que é calculado localmente).
 *
 * @param countryCode ISO 3166-1 alfa-2, ou nulo se o Places não informar o país
 */
public record PlaceCity(String placeId, String name, String countryCode, double lat, double lng) {}
