package com.travelplanner.trip.infra;

import com.travelplanner.trip.domain.Traveler;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TravelerRepository extends JpaRepository<Traveler, Long> {

  List<Traveler> findByOwnerUserId(Long ownerUserId);

  Optional<Traveler> findByIdAndOwnerUserId(Long id, Long ownerUserId);

  /** O viajante "eu" do usuário. */
  @Query("select t from Traveler t where t.ownerUserId = :userId and t.linkedUserId = :userId")
  Optional<Traveler> findSelf(Long userId);

  @Query(
      "select t from Traveler t where t.id in"
          + " (select tt.id.travelerId from TripTraveler tt where tt.id.tripId = :tripId)")
  List<Traveler> findByTripId(Long tripId);
}
