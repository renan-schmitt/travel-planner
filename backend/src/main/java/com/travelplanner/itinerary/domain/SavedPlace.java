package com.travelplanner.itinerary.domain;

import com.travelplanner.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Lugar favorito de uma parada, salvo da busca no Places. Guarda só o {@code placeId} e dados do
 * usuário (o nome é o do momento em que foi salvo); nota e posição vêm do Places sob demanda.
 */
@Getter
@Entity
@Table(name = "saved_place")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SavedPlace extends BaseEntity {

  @Column(name = "stop_id", nullable = false, updatable = false)
  private Long stopId;

  @Column(name = "place_id", nullable = false, updatable = false)
  private String placeId;

  @Column(nullable = false, length = 160)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private SavedPlaceKind kind;

  @Column(name = "price_tier", columnDefinition = "tinyint")
  private Integer priceTier;

  @Enumerated(EnumType.STRING)
  @Column(name = "price_tier_source")
  private PriceTierSource priceTierSource;

  @Column(columnDefinition = "text")
  private String notes;

  @Column(nullable = false)
  private boolean visited;

  /**
   * @param tierSource origem da faixa quando ela é a que a busca mostrou ({@code PLACES}/{@code
   *     HEURISTIC}); nula = informada pelo usuário
   */
  public SavedPlace(
      Long stopId,
      String placeId,
      String name,
      SavedPlaceKind kind,
      Integer priceTier,
      PriceTierSource tierSource,
      String notes) {
    this.stopId = stopId;
    this.placeId = placeId;
    this.name = name;
    this.kind = kind;
    this.priceTier = priceTier;
    this.priceTierSource =
        priceTier == null ? null : Objects.requireNonNullElse(tierSource, PriceTierSource.USER);
    this.notes = notes;
  }

  /** Substitui os campos editáveis; faixa alterada passa a ter origem {@code USER}. */
  public void update(SavedPlaceKind kind, Integer priceTier, String notes, boolean visited) {
    this.kind = kind;
    if (priceTier == null) {
      this.priceTierSource = null;
    } else if (!Objects.equals(priceTier, this.priceTier)) {
      this.priceTierSource = PriceTierSource.USER;
    }
    this.priceTier = priceTier;
    this.notes = notes;
    this.visited = visited;
  }
}
