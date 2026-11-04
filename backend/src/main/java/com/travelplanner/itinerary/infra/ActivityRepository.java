package com.travelplanner.itinerary.infra;

import com.travelplanner.itinerary.domain.Activity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ActivityRepository extends JpaRepository<Activity, Long> {

  List<Activity> findByTripIdOrderByDayAscSortOrderAscIdAsc(Long tripId);

  List<Activity> findByTripIdAndDayOrderBySortOrderAscIdAsc(Long tripId, LocalDate day);

  Optional<Activity> findByIdAndTripId(Long id, Long tripId);

  /** Próxima posição livre no dia (0 se o dia não tem atividades). */
  @Query(
      "select coalesce(max(a.sortOrder) + 1, 0) from Activity a"
          + " where a.tripId = :tripId and a.day = :day")
  int nextSortOrder(Long tripId, LocalDate day);
}
