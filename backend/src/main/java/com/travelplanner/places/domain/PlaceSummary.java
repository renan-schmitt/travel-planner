package com.travelplanner.places.domain;

/**
 * Nome e endereço de um lugar (field mask mínima), para gravar uma hospedagem escolhida na busca.
 */
public record PlaceSummary(String placeId, String name, String address) {}
