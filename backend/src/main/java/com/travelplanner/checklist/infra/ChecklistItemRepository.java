package com.travelplanner.checklist.infra;

import com.travelplanner.checklist.domain.ChecklistItem;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ChecklistItemRepository extends JpaRepository<ChecklistItem, Long> {

  List<ChecklistItem> findByTripIdOrderBySortOrderAscIdAsc(Long tripId);

  Optional<ChecklistItem> findByIdAndTripId(Long id, Long tripId);

  Optional<ChecklistItem> findByTripIdAndRuleCodeAndRuleKey(
      Long tripId, String ruleCode, String ruleKey);

  @Query("select coalesce(max(i.sortOrder), -1) from ChecklistItem i where i.tripId = :tripId")
  int maxSortOrder(Long tripId);

  /** Tira o responsável dos itens da viagem (quando ele sai dela). */
  @Modifying(flushAutomatically = true)
  @Query(
      "update ChecklistItem i set i.assigneeUserId = null, i.version = i.version + 1,"
          + " i.updatedAt = :now where i.tripId = :tripId and i.assigneeUserId = :userId")
  int unassign(Long tripId, Long userId, Instant now);
}
