package com.travelplanner.suggestion.domain.rules;

import com.travelplanner.itinerary.domain.SegmentDetails;
import com.travelplanner.itinerary.domain.TransportType;
import com.travelplanner.itinerary.domain.TripSnapshot.SegmentEntry;
import com.travelplanner.suggestion.domain.GapContext;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/** Utilidades das regras de logística. */
final class RuleSupport {

  private RuleSupport() {}

  static boolean flight(SegmentEntry segment) {
    return segment.details().type() == TransportType.FLIGHT;
  }

  /** "Trecho de chão" = qualquer tipo que não seja avião. */
  static boolean ground(SegmentEntry segment) {
    return segment.details().type() != TransportType.FLIGHT;
  }

  static Instant departure(SegmentEntry segment) {
    return segment.details().departure().toInstant();
  }

  /** Chegada; sem ela, a partida (trecho sem horário de chegada). */
  static Instant arrivalOrDeparture(SegmentEntry segment) {
    SegmentDetails d = segment.details();
    return d.arrival() != null ? d.arrival().toInstant() : d.departure().toInstant();
  }

  /** {@code from <= instant <= to}. */
  static boolean between(Instant instant, Instant from, Instant to) {
    return !instant.isBefore(from) && !instant.isAfter(to);
  }

  /**
   * Parâmetros comuns de um trecho para as mensagens: id, tipo, companhia, código, origem/destino
   * (rótulo digitado ou cidade; ausente = casa) e horários locais com o fuso.
   */
  static Map<String, Object> segmentParams(GapContext context, SegmentEntry segment) {
    SegmentDetails d = segment.details();
    Map<String, Object> params = new LinkedHashMap<>();
    params.put("segmentId", segment.id());
    params.put("type", d.type().name());
    putIfPresent(params, "carrier", d.carrier());
    putIfPresent(params, "code", d.code());
    putIfPresent(params, "origin", place(context, d.originLabel(), d.originStopId()));
    putIfPresent(
        params, "destination", place(context, d.destinationLabel(), d.destinationStopId()));
    params.put("departAt", d.departAt().toString());
    params.put("departTz", d.departTz());
    if (d.arriveAt() != null) {
      params.put("arriveAt", d.arriveAt().toString());
      params.put("arriveTz", d.arriveTz());
    }
    return params;
  }

  /** Identificação curta do voo para títulos de checklist: "TAP TP82" ou "São Paulo → Lisboa". */
  static String flightLabel(GapContext context, SegmentEntry segment) {
    SegmentDetails d = segment.details();
    String byCode = join(d.carrier(), d.code());
    if (!byCode.isEmpty()) {
      return byCode;
    }
    return orHome(context, place(context, d.originLabel(), d.originStopId()))
        + " → "
        + orHome(context, place(context, d.destinationLabel(), d.destinationStopId()));
  }

  static void putIfPresent(Map<String, Object> params, String key, Object value) {
    if (value != null) {
      params.put(key, value);
    }
  }

  private static String place(GapContext context, String label, Long stopId) {
    if (label != null && !label.isBlank()) {
      return label;
    }
    return context.cityOf(stopId);
  }

  private static String orHome(GapContext context, String place) {
    if (place != null) {
      return place;
    }
    var home = context.snapshot().owner().homeCity();
    return home != null && home.name() != null ? home.name() : "Casa";
  }

  private static String join(String a, String b) {
    return ((a == null ? "" : a.trim()) + " " + (b == null ? "" : b.trim())).trim();
  }
}
