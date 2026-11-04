package com.travelplanner.places.domain;

import java.util.List;

/**
 * O que buscar perto de uma parada ({@code POST /places:searchNearby}): tipos do Places (Tabela A),
 * raio e o texto da busca de reserva (parada digitada à mão, sem coordenadas).
 */
public enum NearbyKind {
  RESTAURANT(List.of("restaurant"), 5_000, "restaurants"),
  ATTRACTION(
      List.of("tourist_attraction", "museum", "park", "beach", "historical_landmark"),
      10_000,
      "tourist attractions");

  private final List<String> includedTypes;
  private final double radiusMeters;
  private final String textNoun;

  NearbyKind(List<String> includedTypes, double radiusMeters, String textNoun) {
    this.includedTypes = includedTypes;
    this.radiusMeters = radiusMeters;
    this.textNoun = textNoun;
  }

  public List<String> includedTypes() {
    return includedTypes;
  }

  public double radiusMeters() {
    return radiusMeters;
  }

  /** "restaurants" / "tourist attractions", para "… in Lisboa, Portugal". */
  public String textNoun() {
    return textNoun;
  }
}
