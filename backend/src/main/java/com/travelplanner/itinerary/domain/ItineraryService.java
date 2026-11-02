package com.travelplanner.itinerary.domain;

import com.travelplanner.trip.domain.TripAccessService;
import com.travelplanner.trip.domain.TripRole;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Timeline dia a dia (leitura, {@code VIEWER}). A lógica fica no {@link ItineraryBuilder}. */
@Service
@RequiredArgsConstructor
public class ItineraryService {

  private final TripAccessService access;
  private final TripSnapshotLoader snapshots;

  /** Os dias da viagem e o snapshot de onde vieram (a API resolve nomes de paradas com ele). */
  public record Itinerary(TripSnapshot snapshot, List<ItineraryDay> days) {}

  @Transactional(readOnly = true)
  public Itinerary build(long tripId, long userId) {
    access.requireRole(tripId, userId, TripRole.VIEWER);
    TripSnapshot snapshot = snapshots.load(tripId);
    return new Itinerary(snapshot, ItineraryBuilder.build(snapshot));
  }
}
