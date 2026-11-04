package com.travelplanner.suggestion.infra;

import com.travelplanner.suggestion.domain.Severity;
import com.travelplanner.suggestion.domain.Suggestion;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SuggestionRepository extends JpaRepository<Suggestion, Long> {

  /**
   * Regra visível só para administradores ({@code REQUISITO_DESATUALIZADO}): fica fora das
   * contagens (badges, cards e dashboard), que são as mesmas para todos os membros.
   */
  String ADMIN_ONLY_RULE = "REQUISITO_DESATUALIZADO";

  List<Suggestion> findByTripId(Long tripId);

  Optional<Suggestion> findByIdAndTripId(Long id, Long tripId);

  /** Pendentes por viagem (as adiadas cujo prazo chegou contam). */
  @Query(
      "select s.tripId, count(s) from Suggestion s where s.tripId in :tripIds and (s.status ="
          + " com.travelplanner.suggestion.domain.SuggestionStatus.PENDING or (s.status ="
          + " com.travelplanner.suggestion.domain.SuggestionStatus.SNOOZED and s.snoozedUntil <="
          + " :today)) and s.ruleCode <> '"
          + ADMIN_ONLY_RULE
          + "' group by s.tripId")
  List<Object[]> countPending(Collection<Long> tripIds, LocalDate today);

  /** Pendentes por viagem e severidade (as adiadas cujo prazo chegou contam). */
  @Query(
      "select s.tripId, s.severity, count(s) from Suggestion s where s.tripId in :tripIds and"
          + " (s.status = com.travelplanner.suggestion.domain.SuggestionStatus.PENDING or"
          + " (s.status = com.travelplanner.suggestion.domain.SuggestionStatus.SNOOZED and"
          + " s.snoozedUntil <= :today)) and s.ruleCode <> '"
          + ADMIN_ONLY_RULE
          + "' group by s.tripId, s.severity")
  List<Object[]> countPendingBySeverity(Collection<Long> tripIds, LocalDate today);

  /** Pendentes (efetivas hoje) de uma severidade. */
  @Query(
      "select s from Suggestion s where s.tripId in :tripIds and s.severity = :severity and"
          + " (s.status = com.travelplanner.suggestion.domain.SuggestionStatus.PENDING or"
          + " (s.status = com.travelplanner.suggestion.domain.SuggestionStatus.SNOOZED and"
          + " s.snoozedUntil <= :today)) and s.ruleCode <> '"
          + ADMIN_ONLY_RULE
          + "' order by s.tripId, s.ruleCode, s.id")
  List<Suggestion> findPendingBySeverity(
      Collection<Long> tripIds, Severity severity, LocalDate today);
}
