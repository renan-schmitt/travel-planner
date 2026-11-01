package com.travelplanner.trip.domain;

/** Viagem vista por um usuário: o papel dele e o status efetivo no dia de hoje. */
public record TripView(Trip trip, TripRole role, TripStatus status) {}
