package com.travelplanner.itinerary.domain;

/**
 * De onde veio a faixa de preço: do Places, de uma heurística, da IA ou do próprio usuário. Só
 * {@code USER} existe por enquanto; as outras chegam com a busca de hotéis (Fase 16) e a IA.
 */
public enum PriceTierSource {
  PLACES,
  HEURISTIC,
  AI,
  USER
}
