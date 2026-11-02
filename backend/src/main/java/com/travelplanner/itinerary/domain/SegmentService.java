package com.travelplanner.itinerary.domain;

import com.travelplanner.itinerary.infra.StopRepository;
import com.travelplanner.itinerary.infra.TransportSegmentRepository;
import com.travelplanner.shared.web.InvalidRequestException;
import com.travelplanner.shared.web.NotFoundException;
import com.travelplanner.trip.domain.TripAccessService;
import com.travelplanner.trip.domain.TripChangedEvent;
import com.travelplanner.trip.domain.TripRole;
import com.travelplanner.trip.domain.TripService;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Trechos de transporte da viagem. Leitura exige {@code VIEWER}; alterações, {@code EDITOR}. Toda
 * alteração recalcula as datas derivadas da viagem e publica {@link TripChangedEvent}.
 */
@Service
@RequiredArgsConstructor
public class SegmentService {

  /** Ordem cronológica real (instante da partida), não a do relógio local. */
  private static final Comparator<TransportSegment> CHRONOLOGICAL =
      Comparator.comparing((TransportSegment s) -> s.details().departure().toInstant())
          .thenComparing(TransportSegment::getId);

  private final TransportSegmentRepository segments;
  private final StopRepository stops;
  private final TripAccessService access;
  private final TripService trips;
  private final ApplicationEventPublisher events;

  /** Trechos em ordem cronológica de partida. */
  @Transactional(readOnly = true)
  public List<TransportSegment> list(long tripId, long userId) {
    access.requireRole(tripId, userId, TripRole.VIEWER);
    return segments.findByTripId(tripId).stream().sorted(CHRONOLOGICAL).toList();
  }

  @Transactional
  public TransportSegment create(long tripId, long userId, SegmentDetails details) {
    access.requireRole(tripId, userId, TripRole.EDITOR);
    check(tripId, details);
    TransportSegment segment = segments.saveAndFlush(new TransportSegment(tripId, details));
    changed(tripId);
    return segment;
  }

  @Transactional
  public TransportSegment update(
      long tripId, long segmentId, long userId, long version, SegmentDetails details) {
    access.requireRole(tripId, userId, TripRole.EDITOR);
    TransportSegment segment = find(tripId, segmentId);
    if (segment.getVersion() != version) {
      throw new OptimisticLockingFailureException("Versão do trecho divergente.");
    }
    check(tripId, details);
    segment.update(details);
    segments.saveAndFlush(segment);
    changed(tripId);
    return segment;
  }

  @Transactional
  public void delete(long tripId, long segmentId, long userId) {
    access.requireRole(tripId, userId, TripRole.EDITOR);
    segments.delete(find(tripId, segmentId));
    segments.flush();
    changed(tripId);
  }

  private TransportSegment find(long tripId, long segmentId) {
    return segments
        .findByIdAndTripId(segmentId, tripId)
        .orElseThrow(() -> new NotFoundException("Trecho não encontrado."));
  }

  private void check(long tripId, SegmentDetails details) {
    details.validate();
    requireStop(tripId, details.originStopId(), "originStopId");
    requireStop(tripId, details.destinationStopId(), "destinationStopId");
    requireStop(tripId, details.localStopId(), "localStopId");
  }

  private void requireStop(long tripId, Long stopId, String field) {
    if (stopId != null && stops.findByIdAndTripId(stopId, tripId).isEmpty()) {
      throw new InvalidRequestException(field, "Parada não encontrada nesta viagem.");
    }
  }

  private void changed(long tripId) {
    trips.refreshItineraryDates(tripId);
    events.publishEvent(new TripChangedEvent(tripId));
  }
}
