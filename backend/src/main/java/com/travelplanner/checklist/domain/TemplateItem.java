package com.travelplanner.checklist.domain;

import com.travelplanner.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Item de um {@link ChecklistTemplate}. */
@Getter
@Entity
@Table(name = "template_item")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TemplateItem extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "template_id", nullable = false, updatable = false)
  private ChecklistTemplate template;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ChecklistCategory category;

  @Column(nullable = false, length = 200)
  private String title;

  @Column(name = "due_offset_days")
  private Integer dueOffsetDays;

  @Column(name = "sort_order", nullable = false)
  private int sortOrder;

  TemplateItem(ChecklistTemplate template, TemplateItemDetails details, int sortOrder) {
    this.template = template;
    this.category = details.category();
    this.title = details.title();
    this.dueOffsetDays = details.dueOffsetDays();
    this.sortOrder = sortOrder;
  }

  public TemplateItemDetails details() {
    return new TemplateItemDetails(category, title, dueOffsetDays);
  }
}
