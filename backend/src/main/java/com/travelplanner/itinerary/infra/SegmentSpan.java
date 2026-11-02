package com.travelplanner.itinerary.infra;

import java.time.LocalDateTime;

/**
 * Primeira partida e última chegada (ou partida, se o trecho não tem chegada) dos trechos, no
 * relógio local de cada ponta. Nulas sem trechos.
 */
public record SegmentSpan(LocalDateTime first, LocalDateTime last) {}
