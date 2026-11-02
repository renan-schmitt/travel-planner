package com.travelplanner.itinerary.infra;

import com.travelplanner.itinerary.domain.TransportSegment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TransportSegmentRepository extends JpaRepository<TransportSegment, Long> {

  List<TransportSegment> findByTripId(Long tripId);

  Optional<TransportSegment> findByIdAndTripId(Long id, Long tripId);

  @Query(
      "select new com.travelplanner.itinerary.infra.SegmentSpan("
          + "min(s.departAt), max(coalesce(s.arriveAt, s.departAt)))"
          + " from TransportSegment s where s.tripId = :tripId")
  SegmentSpan span(Long tripId);
}
