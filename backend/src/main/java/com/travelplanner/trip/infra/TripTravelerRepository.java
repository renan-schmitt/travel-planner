package com.travelplanner.trip.infra;

import com.travelplanner.trip.domain.TripTraveler;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface TripTravelerRepository extends JpaRepository<TripTraveler, TripTraveler.Key> {

  @Query("select tt.id.tripId from TripTraveler tt where tt.id.travelerId = :travelerId")
  List<Long> findTripIdsByTravelerId(Long travelerId);

  @Modifying(flushAutomatically = true)
  @Query("delete from TripTraveler tt where tt.id.tripId = :tripId")
  void deleteByTripId(Long tripId);
}
