package com.travelplanner.trip.domain;

/**
 * Viagem vista por um usuário: o papel dele, o status efetivo no dia de hoje, o número de destinos
 * (paradas) e o de sugestões pendentes do motor de lacunas.
 */
public record TripView(
    Trip trip, TripRole role, TripStatus status, int destinationCount, int pendingSuggestions) {}
