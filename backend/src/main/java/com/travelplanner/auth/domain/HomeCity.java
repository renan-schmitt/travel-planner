package com.travelplanner.auth.domain;

/**
 * Cidade de origem do usuário. Até o autocomplete de cidade (Fase 7), só {@code name} e {@code
 * countryCode} são preenchidos; {@code placeId} e {@code timezone} podem ser nulos.
 */
public record HomeCity(String name, String placeId, String countryCode, String timezone) {}
