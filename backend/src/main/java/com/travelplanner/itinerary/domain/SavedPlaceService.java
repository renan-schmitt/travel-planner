package com.travelplanner.itinerary.domain;

import com.travelplanner.itinerary.infra.SavedPlaceRepository;
import com.travelplanner.itinerary.infra.StopRepository;
import com.travelplanner.places.domain.PlaceInfo;
import com.travelplanner.places.domain.PlaceSummary;
import com.travelplanner.places.domain.PlacesService;
import com.travelplanner.places.domain.PriceTier;
import com.travelplanner.shared.web.ConflictException;
import com.travelplanner.shared.web.InvalidRequestException;
import com.travelplanner.shared.web.NotFoundException;
import com.travelplanner.shared.web.ServiceUnavailableException;
import com.travelplanner.trip.domain.TripAccessService;
import com.travelplanner.trip.domain.TripChangedEvent;
import com.travelplanner.trip.domain.TripRole;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lugares favoritos das paradas. Leitura exige {@code VIEWER}; alterações, {@code EDITOR}. Toda
 * alteração publica {@link TripChangedEvent} (as regras {@code RESTAURANTES}/{@code ATRACOES} olham
 * os favoritos).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SavedPlaceService {

  private final SavedPlaceRepository saved;
  private final StopRepository stops;
  private final TripAccessService access;
  private final ApplicationEventPublisher events;
  private final PlacesService places;

  /** Favoritos de todas as paradas da viagem (sem os dados do Places). */
  @Transactional(readOnly = true)
  public List<SavedPlace> listByTrip(long tripId, long userId) {
    access.requireRole(tripId, userId, TripRole.VIEWER);
    return saved.findByTripId(tripId);
  }

  @Transactional(readOnly = true)
  public List<SavedPlace> list(long tripId, long stopId, long userId) {
    access.requireRole(tripId, userId, TripRole.VIEWER);
    findStop(tripId, stopId);
    return saved.findByStopIdOrderByIdAsc(stopId);
  }

  /**
   * Nota, posição e link do Maps de cada favorito, pelo Places (cache de 24 h). Fora de transação:
   * Places fora do ar ou lugar que sumiu só deixa o favorito sem esses dados.
   */
  public Map<String, PlaceInfo> placeInfo(Collection<SavedPlace> list) {
    Map<String, PlaceInfo> info = new LinkedHashMap<>();
    for (SavedPlace place : list) {
      if (info.containsKey(place.getPlaceId())) {
        continue;
      }
      try {
        info.put(place.getPlaceId(), places.getPlaceInfo(place.getPlaceId()));
      } catch (NotFoundException e) {
        log.info("Favorito {} não existe mais no Places", place.getId());
      } catch (ServiceUnavailableException e) {
        // Não insiste nos demais: cada um esperaria o mesmo timeout.
        break;
      }
    }
    return info;
  }

  /**
   * @param name nome mostrado na busca (opcional: sem ele, vem do Places)
   * @param priceTier faixa igual à que a busca mostrou guarda a origem dela; diferente = {@code
   *     USER}
   */
  @Transactional
  public SavedPlace create(
      long tripId,
      long stopId,
      long userId,
      String placeId,
      SavedPlaceKind kind,
      String name,
      Integer priceTier,
      String notes) {
    access.requireRole(tripId, userId, TripRole.EDITOR);
    findStop(tripId, stopId);
    if (saved.existsByStopIdAndPlaceId(stopId, placeId)) {
      throw new ConflictException("Este lugar já está nos favoritos da parada.");
    }
    String finalName = name != null ? name : placeName(placeId);
    PriceTierSource source =
        places
            .searchedTier(placeId)
            .filter(tier -> Objects.equals(tier.tier(), priceTier))
            .map(PriceTier::source)
            .map(s -> PriceTierSource.valueOf(s.name()))
            .orElse(null);
    SavedPlace place =
        saved.saveAndFlush(
            new SavedPlace(stopId, placeId, finalName, kind, priceTier, source, notes));
    changed(tripId);
    return place;
  }

  private String placeName(String placeId) {
    PlaceSummary summary;
    try {
      summary = places.getPlaceSummary(placeId);
    } catch (NotFoundException e) {
      throw new InvalidRequestException("placeId", "Lugar não encontrado no Google.");
    }
    if (summary.name() == null) {
      throw new InvalidRequestException("name", "Informe o nome do lugar.");
    }
    return summary.name().length() > 160 ? summary.name().substring(0, 160) : summary.name();
  }

  @Transactional
  public SavedPlace update(
      long tripId,
      long stopId,
      long savedPlaceId,
      long userId,
      long version,
      SavedPlaceKind kind,
      Integer priceTier,
      String notes,
      boolean visited) {
    access.requireRole(tripId, userId, TripRole.EDITOR);
    findStop(tripId, stopId);
    SavedPlace place = find(stopId, savedPlaceId);
    if (place.getVersion() != version) {
      throw new OptimisticLockingFailureException("Versão do favorito divergente.");
    }
    place.update(kind, priceTier, notes, visited);
    saved.saveAndFlush(place);
    changed(tripId);
    return place;
  }

  @Transactional
  public void delete(long tripId, long stopId, long savedPlaceId, long userId) {
    access.requireRole(tripId, userId, TripRole.EDITOR);
    findStop(tripId, stopId);
    saved.delete(find(stopId, savedPlaceId));
    saved.flush();
    changed(tripId);
  }

  private Stop findStop(long tripId, long stopId) {
    return stops
        .findByIdAndTripId(stopId, tripId)
        .orElseThrow(() -> new NotFoundException("Parada não encontrada."));
  }

  private SavedPlace find(long stopId, long savedPlaceId) {
    return saved
        .findByIdAndStopId(savedPlaceId, stopId)
        .orElseThrow(() -> new NotFoundException("Favorito não encontrado."));
  }

  private void changed(long tripId) {
    events.publishEvent(new TripChangedEvent(tripId));
  }
}
