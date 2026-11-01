package com.travelplanner.itinerary.infra;

import com.travelplanner.itinerary.domain.Stop;
import com.travelplanner.trip.domain.DateSpan;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface StopRepository extends JpaRepository<Stop, Long> {

  List<Stop> findByTripIdOrderBySeq(Long tripId);

  Optional<Stop> findByIdAndTripId(Long id, Long tripId);

  /** Linhas {@code [tripId, total]}. */
  @Query("select s.tripId, count(s) from Stop s where s.tripId in :tripIds group by s.tripId")
  List<Object[]> countByTripIds(Collection<Long> tripIds);

  @Query(
      "select new com.travelplanner.trip.domain.DateSpan(min(s.arrivalDate), max(s.departureDate))"
          + " from Stop s where s.tripId = :tripId")
  DateSpan dateSpan(Long tripId);
}
