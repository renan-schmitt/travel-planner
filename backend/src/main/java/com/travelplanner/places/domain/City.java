package com.travelplanner.places.domain;

/**
 * Cidade escolhida no autocomplete, pronta para virar uma parada ou a cidade de origem.
 *
 * @param countryCode ISO 3166-1 alfa-2; nulo se o Places não informar
 * @param timezone fuso IANA calculado das coordenadas; nulo se não for possível (ex.: alto-mar)
 */
public record City(
    String placeId, String name, String countryCode, double lat, double lng, String timezone) {}
