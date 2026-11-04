package com.travelplanner.itinerary.domain;

import com.travelplanner.itinerary.infra.LodgingRepository;
import com.travelplanner.itinerary.infra.StopRepository;
import com.travelplanner.places.domain.NearbyKind;
import com.travelplanner.places.domain.NearbyQuery;
import com.travelplanner.places.domain.PlaceInfo;
import com.travelplanner.places.domain.PlaceResult;
import com.travelplanner.places.domain.PlacesService;
import com.travelplanner.shared.web.NotFoundException;
import com.travelplanner.shared.web.ServiceUnavailableException;
import com.travelplanner.trip.domain.TripAccessService;
import com.travelplanner.trip.domain.TripRole;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Busca de restaurantes e atrações perto de uma parada (ESP §3.2). Leitura ({@code VIEWER}); os
 * resultados não são gravados. Centro: a primeira hospedagem da parada ligada ao Places (posição
 * buscada sob demanda) ou, sem ela, o centro da cidade.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlaceSearchService {

  private final StopRepository stops;
  private final LodgingRepository lodgings;
  private final TripAccessService access;
  private final PlacesService places;

  /** Resultado da busca e de onde ela partiu. */
  public record Result(List<PlaceResult> places, Center center) {}

  /**
   * Centro da busca. {@code LODGING}: a hospedagem {@code lodgingName}; {@code CITY}: o centro da
   * cidade; {@code TEXT}: parada sem coordenadas, busca por texto na cidade.
   */
  public record Center(CenterKind kind, Double lat, Double lng, String lodgingName) {}

  public enum CenterKind {
    LODGING,
    CITY,
    TEXT
  }

  /**
   * @param tiers faixas aceitas (vazio = todas); resultados sem faixa saem quando há filtro
   * @param minRating nota mínima (o Nearby não filtra por nota: o filtro é aplicado aqui)
   */
  public Result search(
      long tripId,
      long stopId,
      long userId,
      NearbyKind kind,
      Double minRating,
      Set<Integer> tiers) {
    access.requireRole(tripId, userId, TripRole.VIEWER);
    Stop stop =
        stops
            .findByIdAndTripId(stopId, tripId)
            .orElseThrow(() -> new NotFoundException("Parada não encontrada."));
    Center center = center(stop);
    List<PlaceResult> found =
        places.searchNearby(
            new NearbyQuery(
                kind, center.lat(), center.lng(), stop.getCityName(), stop.getCountryCode()));
    return new Result(
        found.stream()
            .filter(p -> minRating == null || (p.rating() != null && p.rating() >= minRating))
            .filter(
                p ->
                    tiers.isEmpty()
                        || (p.priceTier() != null && tiers.contains(p.priceTier().tier())))
            .toList(),
        center);
  }

  private Center center(Stop stop) {
    Optional<Lodging> lodging =
        lodgings.findByStopIdOrderByCheckInAscIdAsc(stop.getId()).stream()
            .filter(l -> l.getPlaceId() != null)
            .findFirst();
    if (lodging.isPresent()) {
      try {
        PlaceInfo info = places.getPlaceInfo(lodging.get().getPlaceId());
        if (info.lat() != null && info.lng() != null) {
          return new Center(CenterKind.LODGING, info.lat(), info.lng(), lodging.get().getName());
        }
      } catch (NotFoundException | ServiceUnavailableException e) {
        log.info(
            "Sem a posição da hospedagem {}; buscando pelo centro da cidade",
            lodging.get().getId());
      }
    }
    if (stop.getLat() != null && stop.getLng() != null) {
      return new Center(CenterKind.CITY, toDouble(stop.getLat()), toDouble(stop.getLng()), null);
    }
    return new Center(CenterKind.TEXT, null, null, null);
  }

  private static Double toDouble(BigDecimal value) {
    return value == null ? null : value.doubleValue();
  }
}
