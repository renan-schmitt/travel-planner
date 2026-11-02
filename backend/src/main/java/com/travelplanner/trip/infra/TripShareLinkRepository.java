package com.travelplanner.trip.infra;

import com.travelplanner.trip.domain.TripShareLink;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TripShareLinkRepository extends JpaRepository<TripShareLink, Long> {

  List<TripShareLink> findByTripIdAndRevokedAtIsNullOrderByIdAsc(Long tripId);

  Optional<TripShareLink> findByIdAndTripId(Long id, Long tripId);

  Optional<TripShareLink> findByTokenAndRevokedAtIsNull(String token);
}
