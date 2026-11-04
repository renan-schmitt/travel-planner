package com.travelplanner.itinerary.infra;

import com.travelplanner.itinerary.domain.SavedPlace;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SavedPlaceRepository extends JpaRepository<SavedPlace, Long> {

  List<SavedPlace> findByStopIdOrderByIdAsc(Long stopId);

  Optional<SavedPlace> findByIdAndStopId(Long id, Long stopId);

  boolean existsByStopIdAndPlaceId(Long stopId, String placeId);

  /** Favoritos de todas as paradas da viagem, por parada e ordem de criação. */
  @Query(
      "select p from SavedPlace p where p.stopId in"
          + " (select s.id from Stop s where s.tripId = :tripId) order by p.stopId, p.id")
  List<SavedPlace> findByTripId(Long tripId);
}
