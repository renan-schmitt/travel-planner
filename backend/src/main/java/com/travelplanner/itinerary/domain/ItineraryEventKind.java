package com.travelplanner.itinerary.domain;

/**
 * Tipo de evento de um dia da timeline. Um trecho que parte e chega no mesmo dia (ou sem chegada)
 * vira um só {@code SEGMENT}; um que chega em outro dia, {@code SEGMENT_DEPARTURE} e {@code
 * SEGMENT_ARRIVAL}. {@code ACTIVITY} = atividade planejada (Fase 18).
 */
public enum ItineraryEventKind {
  CHECK_OUT,
  SEGMENT,
  SEGMENT_DEPARTURE,
  SEGMENT_ARRIVAL,
  CHECK_IN,
  ACTIVITY
}
