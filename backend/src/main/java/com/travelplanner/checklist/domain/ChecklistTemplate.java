package com.travelplanner.checklist.domain;

import com.travelplanner.shared.persistence.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Template de checklist. Sem dono ({@code ownerUserId} nulo) é do sistema: todos veem, ninguém
 * edita. Os do usuário só aparecem para ele.
 */
@Getter
@Entity
@Table(name = "checklist_template")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChecklistTemplate extends BaseEntity {

  @Column(name = "owner_user_id", updatable = false)
  private Long ownerUserId;

  @Column(nullable = false, length = 120)
  private String name;

  @Column(length = 500)
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private TemplateIcon icon;

  @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("sortOrder ASC, id ASC")
  private List<TemplateItem> items = new ArrayList<>();

  public ChecklistTemplate(long ownerUserId, TemplateDetails details) {
    this.ownerUserId = ownerUserId;
    update(details);
  }

  public boolean isSystem() {
    return ownerUserId == null;
  }

  /** Substitui nome, descrição, ícone e todos os itens. */
  public void update(TemplateDetails details) {
    this.name = details.name();
    this.description = details.description();
    this.icon = details.icon();
    items.clear();
    for (int i = 0; i < details.items().size(); i++) {
      items.add(new TemplateItem(this, details.items().get(i), i));
    }
  }
}
