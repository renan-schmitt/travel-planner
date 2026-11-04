package com.travelplanner.itinerary.domain;

/**
 * De onde veio a faixa de preço: do Places, de uma heurística pelo tipo do lugar ("estimado"), da
 * IA (Fase 25) ou do próprio usuário.
 */
public enum PriceTierSource {
  PLACES,
  HEURISTIC,
  AI,
  USER
}
