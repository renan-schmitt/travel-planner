package com.travelplanner.places.domain;

/**
 * Sugestão do autocomplete de cidades.
 *
 * @param name nome da cidade ("Lisboa")
 * @param description complemento para distinguir homônimas ("Portugal"); pode ser nulo
 */
public record CitySuggestion(String placeId, String name, String description) {}
