package com.travelplanner.itinerary.domain;

/**
 * Onde se dorme na noite de um dia: numa hospedagem, em trânsito (trecho que atravessa a noite), em
 * casa (antes da primeira parada ou depois da última) ou {@code MISSING} = lacuna (noite dentro do
 * roteiro sem hospedagem nem trecho).
 */
public enum OvernightKind {
  LODGING,
  IN_TRANSIT,
  HOME,
  MISSING
}
