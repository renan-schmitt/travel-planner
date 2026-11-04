package com.travelplanner.places.domain;

/**
 * Dados de um lugar buscados sob demanda (os termos do Google não permitem guardá-los no banco):
 * nota, posição e link do Maps. Usado nos favoritos e para centrar a busca na hospedagem.
 */
public record PlaceInfo(
    String placeId,
    Double rating,
    Integer userRatingCount,
    Double lat,
    Double lng,
    String googleMapsUri) {}
