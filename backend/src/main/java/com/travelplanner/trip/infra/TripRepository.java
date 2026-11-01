package com.travelplanner.trip.infra;

import com.travelplanner.trip.domain.Trip;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TripRepository extends JpaRepository<Trip, Long> {

  long countByOwnerId(Long ownerId);
}
