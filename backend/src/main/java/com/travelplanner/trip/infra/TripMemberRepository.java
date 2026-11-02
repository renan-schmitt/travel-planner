package com.travelplanner.trip.infra;

import com.travelplanner.trip.domain.TripMember;
import com.travelplanner.trip.domain.TripRole;
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

  /** Membros com conta (não conta convites pendentes) com o papel. */
  long countByTripIdAndRoleAndUserIdIsNotNull(Long tripId, TripRole role);

  List<TripMember> findByTripIdOrderByIdAsc(Long tripId);

  Optional<TripMember> findByIdAndTripId(Long id, Long tripId);

  /** Convite (pendente ou não) para o e-mail na viagem; a collation não diferencia maiúsculas. */
  Optional<TripMember> findByTripIdAndInviteEmail(Long tripId, String inviteEmail);

  /** Convites ainda pendentes para o e-mail, em qualquer viagem (com a viagem carregada). */
  @Query(
      "select m from TripMember m join fetch m.trip where m.userId is null and m.inviteEmail = :email")
  List<TripMember> findPendingByInviteEmail(String email);
}
