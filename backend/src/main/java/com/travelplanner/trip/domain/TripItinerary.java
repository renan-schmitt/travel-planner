package com.travelplanner.trip.domain;

import java.util.Collection;
import java.util.Map;

/**
 * O que a viagem precisa saber do roteiro (implementado pelo módulo {@code itinerary}, que depende
 * de {@code trip}; a interface fica aqui para não criar dependência circular).
 */
public interface TripItinerary {

  /** Número de paradas por viagem (viagens sem paradas ficam fora do mapa). */
  Map<Long, Integer> countDestinations(Collection<Long> tripIds);

  /** Primeira chegada e última saída das paradas da viagem. */
  DateSpan dateSpan(long tripId);
}
