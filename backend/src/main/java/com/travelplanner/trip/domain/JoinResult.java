package com.travelplanner.trip.domain;

/** Resultado de entrar por um link: a viagem e o papel do usuário nela (depois de entrar). */
public record JoinResult(long tripId, TripRole role) {}
