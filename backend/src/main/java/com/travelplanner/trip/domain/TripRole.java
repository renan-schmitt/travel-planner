package com.travelplanner.trip.domain;

/** Papel do membro na viagem, em ordem crescente de permissão: VIEWER &lt; EDITOR &lt; OWNER. */
public enum TripRole {
  VIEWER,
  EDITOR,
  OWNER;

  /** {@code true} se este papel tem pelo menos as permissões de {@code required}. */
  public boolean atLeast(TripRole required) {
    return compareTo(required) >= 0;
  }

  /** O maior dos dois papéis. */
  public TripRole max(TripRole other) {
    return atLeast(other) ? this : other;
  }
}
