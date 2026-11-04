package com.travelplanner.places.domain;

/**
 * Lugar encontrado numa busca do Places (hospedagem, restaurante, atração). Não é persistido (só
 * vai para a tela e para o cache).
 *
 * @param priceTier faixa de preço (do Places ou estimada); nula se não houver como estimar
 * @param photoName nome da primeira foto ({@code places/…/photos/…}), para o proxy de fotos
 */
public record PlaceResult(
    String placeId,
    String name,
    String address,
    Double lat,
    Double lng,
    Double rating,
    Integer userRatingCount,
    PriceTier priceTier,
    String primaryType,
    String photoName,
    String websiteUri,
    String googleMapsUri) {}
