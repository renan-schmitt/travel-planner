package com.travelplanner.trip.infra;

import com.travelplanner.trip.domain.Trip;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface TripRepository extends JpaRepository<Trip, Long> {

  long countByOwnerId(Long ownerId);

  /**
   * Trava a linha da viagem até o fim da transação ({@code SELECT ... FOR UPDATE}), para duas
   * execuções do motor de lacunas na mesma viagem não correrem juntas.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select t from Trip t where t.id = :id")
  Optional<Trip> lockById(Long id);

  /**
   * Ids das viagens que ainda não terminaram (sem data de fim ou com fim a partir de {@code day}).
   */
  @Query("select t.id from Trip t where t.endDate is null or t.endDate >= :day order by t.id")
  List<Long> findIdsNotEndedBefore(LocalDate day);
}
