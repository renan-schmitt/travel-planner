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

  /**
   * Os dias da viagem, as atividades que ficaram fora deles e o snapshot de onde vieram (a API
   * resolve nomes de paradas e favoritos com ele).
   */
  public record Itinerary(
      TripSnapshot snapshot, List<ItineraryDay> days, List<TripSnapshot.ActivityEntry> outside) {}

  @Transactional(readOnly = true)
  public Itinerary build(long tripId, long userId) {
    access.requireRole(tripId, userId, TripRole.VIEWER);
    TripSnapshot snapshot = snapshots.load(tripId);
    List<ItineraryDay> days = ItineraryBuilder.build(snapshot);
    return new Itinerary(snapshot, days, ItineraryBuilder.outside(snapshot, days));
  }
}
