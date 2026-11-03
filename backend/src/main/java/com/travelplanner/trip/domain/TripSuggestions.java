package com.travelplanner.trip.domain;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;

/**
 * O que a viagem precisa saber das sugestões do motor de lacunas (implementado pelo módulo {@code
 * suggestion}, que depende de {@code trip}; a interface fica aqui para não criar dependência
 * circular).
 */
public interface TripSuggestions {

  /** Sugestões pendentes por viagem (viagens sem pendências ficam fora do mapa). */
  Map<Long, Integer> countPending(Collection<Long> tripIds, LocalDate today);
}
