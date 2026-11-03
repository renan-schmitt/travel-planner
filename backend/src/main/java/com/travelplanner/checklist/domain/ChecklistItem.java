package com.travelplanner.checklist.domain;

import com.travelplanner.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Item de checklist de uma viagem. */
@Getter
@Entity
@Table(name = "checklist_item")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChecklistItem extends BaseEntity {

  @Column(name = "trip_id", nullable = false, updatable = false)
  private Long tripId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ChecklistCategory category;

  @Column(nullable = false, length = 200)
  private String title;

  @Column(columnDefinition = "text")
  private String notes;

  @Column(name = "due_date")
  private LocalDate dueDate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ChecklistStatus status;

  @Column(name = "done_at")
  private Instant doneAt;

  @Column(name = "done_by")
  private Long doneBy;

  @Column(name = "assignee_user_id")
  private Long assigneeUserId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, updatable = false)
  private ChecklistSource source;

  @Column(name = "rule_code", length = 64, updatable = false)
  private String ruleCode;

  @Column(name = "rule_key", length = 160, updatable = false)
  private String ruleKey;

  @Column(name = "sort_order", nullable = false)
  private int sortOrder;

  public ChecklistItem(
      Long tripId, ChecklistSource source, ChecklistItemDetails details, int sortOrder) {
    this.tripId = tripId;
    this.source = source;
    this.status = ChecklistStatus.OPEN;
    this.sortOrder = sortOrder;
    update(details);
  }

  public void update(ChecklistItemDetails details) {
    this.category = details.category();
    this.title = details.title();
    this.notes = details.notes();
    this.dueDate = details.dueDate();
    this.assigneeUserId = details.assigneeUserId();
  }

  /**
   * Marca como concluído ou aberto. Idempotente: repetir o mesmo status não muda quem concluiu nem
   * quando.
   *
   * @return se o status mudou
   */
  public boolean changeStatus(ChecklistStatus status, long userId, Instant now) {
    if (this.status == status) {
      return false;
    }
    this.status = status;
    this.doneAt = status == ChecklistStatus.DONE ? now : null;
    this.doneBy = status == ChecklistStatus.DONE ? userId : null;
    return true;
  }

  public void moveTo(int sortOrder) {
    this.sortOrder = sortOrder;
  }

  public void unassign() {
    this.assigneeUserId = null;
  }
}
