package com.travelplanner.itinerary.domain;

import com.travelplanner.itinerary.infra.LodgingRepository;
import com.travelplanner.itinerary.infra.StopRepository;
import com.travelplanner.shared.web.NotFoundException;
import com.travelplanner.trip.domain.TripAccessService;
import com.travelplanner.trip.domain.TripChangedEvent;
import com.travelplanner.trip.domain.TripRole;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hospedagens das paradas. Leitura exige {@code VIEWER}; alterações, {@code EDITOR}. As datas ficam
 * dentro das da parada, então não mudam o período da viagem; toda alteração publica {@link
 * TripChangedEvent}.
 */
@Service
@RequiredArgsConstructor
public class LodgingService {

  private final LodgingRepository lodgings;
  private final StopRepository stops;
  private final TripAccessService access;
  private final ApplicationEventPublisher events;

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

  @Transactional
  public Lodging create(long tripId, long stopId, long userId, LodgingDetails details) {
    access.requireRole(tripId, userId, TripRole.EDITOR);
    Stop stop = findStop(tripId, stopId);
    details.validate(stop.getArrivalDate(), stop.getDepartureDate());
    Lodging lodging = lodgings.saveAndFlush(new Lodging(stopId, details));
    changed(tripId);
    return lodging;
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
