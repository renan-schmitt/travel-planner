package com.travelplanner.trip.domain;

/** Filtro da listagem: passadas são as {@code DONE}; próximas, todas as outras. */
public enum TripScope {
  UPCOMING,
  PAST,
  ALL
}
