package com.travelplanner.trip.infra;

import com.travelplanner.trip.domain.TripMember;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TripMemberRepository extends JpaRepository<TripMember, Long> {

  Optional<TripMember> findByTripIdAndUserId(Long tripId, Long userId);

  /** Viagens do usuário (com a viagem já carregada), para a listagem. */
  @Query("select m from TripMember m join fetch m.trip where m.userId = :userId")
  List<TripMember> findAllWithTripByUserId(Long userId);

  long countByTripId(Long tripId);
}
