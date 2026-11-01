package com.travelplanner.trip.domain;

/**
 * Viagem vista por um usuário: o papel dele, o status efetivo no dia de hoje e o número de destinos
 * (paradas).
 */
public record TripView(Trip trip, TripRole role, TripStatus status, int destinationCount) {}
