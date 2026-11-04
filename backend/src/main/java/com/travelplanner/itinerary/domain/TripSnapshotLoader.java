package com.travelplanner.itinerary.domain;

import com.travelplanner.auth.domain.AppUser;
import com.travelplanner.auth.domain.AppUserService;
import com.travelplanner.itinerary.infra.LodgingRepository;
import com.travelplanner.itinerary.infra.SavedPlaceRepository;
import com.travelplanner.itinerary.infra.StopRepository;
import com.travelplanner.itinerary.infra.TransportSegmentRepository;
import com.travelplanner.shared.web.NotFoundException;
import com.travelplanner.trip.domain.Traveler;
import com.travelplanner.trip.domain.TravelerDetails;
import com.travelplanner.trip.domain.Trip;
import com.travelplanner.trip.infra.TravelerRepository;
import com.travelplanner.trip.infra.TripRepository;
import java.util.Comparator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Monta o {@link TripSnapshot} de uma viagem. Não verifica acesso: quem chama já verificou (ou é um
 * processo interno, como o motor de lacunas).
 */
@Component
@RequiredArgsConstructor
public class TripSnapshotLoader {

  private final TripRepository trips;
  private final AppUserService users;
  private final StopRepository stops;
  private final TransportSegmentRepository segments;
  private final LodgingRepository lodgings;
  private final TravelerRepository travelers;
  private final SavedPlaceRepository savedPlaces;

  @Transactional(readOnly = true)
  public TripSnapshot load(long tripId) {
    Trip trip =
        trips.findById(tripId).orElseThrow(() -> new NotFoundException("Viagem não encontrada."));
    AppUser owner = users.get(trip.getOwnerId());
    return new TripSnapshot(
        new TripSnapshot.TripInfo(
            trip.getId(), trip.getName(), trip.getStartDate(), trip.getEndDate()),
        new TripSnapshot.Owner(owner.getId(), owner.getNationality(), owner.getHomeCity()),
        stops.findByTripIdOrderBySeq(tripId).stream()
            .map(
                s ->
                    new TripSnapshot.StopEntry(
                        s.getId(),
                        s.getSeq(),
                        new StopDetails(
                            s.getCityPlaceId(),
                            s.getCityName(),
                            s.getCountryCode(),
                            s.getLat(),
                            s.getLng(),
                            s.getTimezone(),
                            s.getArrivalDate(),
                            s.getDepartureDate(),
                            s.getNotes())))
            .toList(),
        segments.findByTripId(tripId).stream()
            .sorted(Comparator.comparing(s -> s.details().departure().toInstant()))
            .map(s -> new TripSnapshot.SegmentEntry(s.getId(), s.details()))
            .toList(),
        lodgings.findByTripId(tripId).stream()
            .map(
                l ->
                    new TripSnapshot.LodgingEntry(
                        l.getId(), l.getStopId(), l.getPlaceId(), l.details()))
            .toList(),
        travelers.findByTripId(tripId).stream()
            .sorted(Comparator.comparing(Traveler::getId))
            .map(
                t ->
                    new TripSnapshot.TravelerEntry(
                        t.getId(),
                        new TravelerDetails(
                            t.getName(),
                            t.getAgeGroup(),
                            t.getBirthYear(),
                            t.getNationalities(),
                            t.getPassportExpiry())))
            .toList(),
        savedPlaces.findByTripId(tripId).stream()
            .map(
                p ->
                    new TripSnapshot.SavedPlaceEntry(
                        p.getId(), p.getStopId(), p.getPlaceId(), p.getKind()))
            .toList());
  }
}
