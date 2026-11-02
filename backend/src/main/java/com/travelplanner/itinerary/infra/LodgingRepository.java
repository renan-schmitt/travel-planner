package com.travelplanner.itinerary.infra;

import com.travelplanner.itinerary.domain.Lodging;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface LodgingRepository extends JpaRepository<Lodging, Long> {

  List<Lodging> findByStopIdOrderByCheckInAscIdAsc(Long stopId);

  Optional<Lodging> findByIdAndStopId(Long id, Long stopId);

  /** Hospedagens de todas as paradas da viagem, por check-in. */
  @Query(
      "select l from Lodging l where l.stopId in"
          + " (select s.id from Stop s where s.tripId = :tripId) order by l.checkIn, l.id")
  List<Lodging> findByTripId(Long tripId);
}
