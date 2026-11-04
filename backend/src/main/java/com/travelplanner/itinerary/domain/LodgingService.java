package com.travelplanner.itinerary.domain;

import com.travelplanner.itinerary.infra.LodgingRepository;
import com.travelplanner.itinerary.infra.StopRepository;
import com.travelplanner.places.domain.PlaceSummary;
import com.travelplanner.places.domain.PlacesService;
import com.travelplanner.places.domain.PriceTier;
import com.travelplanner.shared.web.InvalidRequestException;
import com.travelplanner.shared.web.NotFoundException;
import com.travelplanner.shared.web.ServiceUnavailableException;
import com.travelplanner.trip.domain.TripAccessService;
import com.travelplanner.trip.domain.TripChangedEvent;
import com.travelplanner.trip.domain.TripRole;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hospedagens das paradas. Leitura exige {@code VIEWER}; alterações, {@code EDITOR}. As datas ficam
 * dentro das da parada, então não mudam o período da viagem; toda alteração publica {@link
 * TripChangedEvent}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LodgingService {

  private final LodgingRepository lodgings;
  private final StopRepository stops;
  private final TripAccessService access;
  private final ApplicationEventPublisher events;
  private final PlacesService places;

  /** Hospedagens de todas as paradas da viagem, por check-in. */
  @Transactional(readOnly = true)
  public List<Lodging> listByTrip(long tripId, long userId) {
    access.requireRole(tripId, userId, TripRole.VIEWER);
    return lodgings.findByTripId(tripId);
  }

  @Transactional(readOnly = true)
  public List<Lodging> list(long tripId, long stopId, long userId) {
    access.requireRole(tripId, userId, TripRole.VIEWER);
    findStop(tripId, stopId);
    return lodgings.findByStopIdOrderByCheckInAscIdAsc(stopId);
  }

  /**
   * @param placeId lugar do Places escolhido na busca de hotéis (opcional). Nome e endereço vêm do
   *     Places quando não informados; a faixa igual à que a busca mostrou guarda a origem dela.
   */
  @Transactional
  public Lodging create(
      long tripId, long stopId, long userId, LodgingDetails details, String placeId) {
    access.requireRole(tripId, userId, TripRole.EDITOR);
    Stop stop = findStop(tripId, stopId);
    LodgingDetails filled = placeId == null ? details : withPlace(details, placeId);
    if (filled.name() == null) {
      throw new InvalidRequestException("name", "Informe o nome da hospedagem.");
    }
    filled.validate(stop.getArrivalDate(), stop.getDepartureDate());
    Lodging lodging = new Lodging(stopId, filled);
    if (placeId != null) {
      Optional<PriceTier> searched = places.searchedTier(placeId);
      lodging.linkPlace(
          placeId,
          searched
              .filter(tier -> Objects.equals(tier.tier(), filled.priceTier()))
              .map(tier -> PriceTierSource.valueOf(tier.source().name()))
              .orElse(null));
    }
    lodging = lodgings.saveAndFlush(lodging);
    changed(tripId);
    return lodging;
  }

  /**
   * Completa nome e endereço com o Places. Se o usuário já mandou o nome e o Places está fora do
   * ar, segue sem ele; lugar inexistente → 400 em {@code placeId}.
   */
  private LodgingDetails withPlace(LodgingDetails details, String placeId) {
    PlaceSummary place;
    try {
      place = places.getPlaceSummary(placeId);
    } catch (NotFoundException e) {
      throw new InvalidRequestException("placeId", "Lugar não encontrado no Google.");
    } catch (ServiceUnavailableException e) {
      if (details.name() == null) {
        throw e;
      }
      log.warn(
          "Places indisponível ao adicionar a hospedagem {}; usando o nome informado", placeId);
      return details;
    }
    return details.withPlace(
        Objects.requireNonNullElse(details.name(), place.name()),
        Objects.requireNonNullElse(details.address(), place.address()));
  }

  @Transactional
  public Lodging update(
      long tripId, long stopId, long lodgingId, long userId, long version, LodgingDetails details) {
    access.requireRole(tripId, userId, TripRole.EDITOR);
    Stop stop = findStop(tripId, stopId);
    Lodging lodging = find(stopId, lodgingId);
    if (lodging.getVersion() != version) {
      throw new OptimisticLockingFailureException("Versão da hospedagem divergente.");
    }
    details.validate(stop.getArrivalDate(), stop.getDepartureDate());
    lodging.update(details);
    lodgings.saveAndFlush(lodging);
    changed(tripId);
    return lodging;
  }

  @Transactional
  public void delete(long tripId, long stopId, long lodgingId, long userId) {
    access.requireRole(tripId, userId, TripRole.EDITOR);
    findStop(tripId, stopId);
    lodgings.delete(find(stopId, lodgingId));
    lodgings.flush();
    changed(tripId);
  }

  private Stop findStop(long tripId, long stopId) {
    return stops
        .findByIdAndTripId(stopId, tripId)
        .orElseThrow(() -> new NotFoundException("Parada não encontrada."));
  }

  private Lodging find(long stopId, long lodgingId) {
    return lodgings
        .findByIdAndStopId(lodgingId, stopId)
        .orElseThrow(() -> new NotFoundException("Hospedagem não encontrada."));
  }

  private void changed(long tripId) {
    events.publishEvent(new TripChangedEvent(tripId));
  }
}
