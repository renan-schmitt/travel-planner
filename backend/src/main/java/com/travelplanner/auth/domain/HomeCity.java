package com.travelplanner.auth.domain;

/**
 * Cidade de origem do usuário. Escolhida no autocomplete, tem {@code placeId} e {@code timezone};
 * digitada à mão (Places indisponível) ou cadastrada antes da Fase 7, eles podem ser nulos.
 */
public record HomeCity(String name, String placeId, String countryCode, String timezone) {}
