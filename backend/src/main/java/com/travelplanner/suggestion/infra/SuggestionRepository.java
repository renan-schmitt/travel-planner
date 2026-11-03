package com.travelplanner.suggestion.infra;

import com.travelplanner.suggestion.domain.Suggestion;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SuggestionRepository extends JpaRepository<Suggestion, Long> {

  List<Suggestion> findByTripId(Long tripId);

  Optional<Suggestion> findByIdAndTripId(Long id, Long tripId);

  /** Pendentes por viagem (as adiadas cujo prazo chegou contam). */
  @Query(
      "select s.tripId, count(s) from Suggestion s where s.tripId in :tripIds and (s.status ="
          + " com.travelplanner.suggestion.domain.SuggestionStatus.PENDING or (s.status ="
          + " com.travelplanner.suggestion.domain.SuggestionStatus.SNOOZED and s.snoozedUntil <="
          + " :today)) group by s.tripId")
  List<Object[]> countPending(Collection<Long> tripIds, LocalDate today);
}
