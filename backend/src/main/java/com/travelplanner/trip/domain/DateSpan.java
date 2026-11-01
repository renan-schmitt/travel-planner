package com.travelplanner.trip.domain;

import java.time.LocalDate;

/** Período coberto pelo roteiro (datas nulas se ainda não houver paradas). */
public record DateSpan(LocalDate start, LocalDate end) {}
