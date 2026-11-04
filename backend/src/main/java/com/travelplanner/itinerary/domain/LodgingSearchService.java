package com.travelplanner.itinerary.domain;

import com.travelplanner.itinerary.infra.StopRepository;
import com.travelplanner.places.domain.LodgingPage;
import com.travelplanner.places.domain.LodgingQuery;
import com.travelplanner.places.domain.LodgingType;
import com.travelplanner.places.domain.PlacesService;
import com.travelplanner.shared.web.NotFoundException;
import com.travelplanner.trip.domain.TripAccessService;
import com.travelplanner.trip.domain.TripRole;
import java.math.BigDecimal;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Busca de hotéis/resorts no Places perto de uma parada (ESP §3.1). Leitura ({@code VIEWER}); os
 * resultados não são gravados.
 */
@Service
@RequiredArgsConstructor
public class LodgingSearchService {

  private final StopRepository stops;
  private final TripAccessService access;
  private final PlacesService places;

  /**
   * @param tiers faixas aceitas (vazio = todas). O filtro é aplicado aqui, depois do mapeamento,
   *     para valer também a faixa estimada; resultados sem faixa saem quando há filtro.
   */
  @Transactional(readOnly = true)
  public LodgingPage search(
      long tripId,
      long stopId,
      long userId,
      LodgingType type,
      Double minRating,
      Set<Integer> tiers,
      String pageToken) {
    access.requireRole(tripId, userId, TripRole.VIEWER);
    Stop stop =
        stops
            .findByIdAndTripId(stopId, tripId)
            .orElseThrow(() -> new NotFoundException("Parada não encontrada."));
    LodgingPage page =
        places.searchLodging(
            new LodgingQuery(
                stop.getCityName(),
                stop.getCountryCode(),
                toDouble(stop.getLat()),
                toDouble(stop.getLng()),
                type,
                minRating,
                pageToken));
    if (tiers.isEmpty()) {
      return page;
    }
    return new LodgingPage(
        page.places().stream()
            .filter(p -> p.priceTier() != null && tiers.contains(p.priceTier().tier()))
            .toList(),
        page.nextPageToken());
  }

  private static Double toDouble(BigDecimal value) {
    return value == null ? null : value.doubleValue();
  }
}
