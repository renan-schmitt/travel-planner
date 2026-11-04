package com.travelplanner.places.domain;

/**
 * Faixa de preço de 1 ({@code $}) a 4 ({@code $$$$}) e de onde ela veio.
 *
 * @param tier 1–4
 */
public record PriceTier(int tier, TierSource source) {

  public boolean estimated() {
    return source == TierSource.HEURISTIC;
  }
}
