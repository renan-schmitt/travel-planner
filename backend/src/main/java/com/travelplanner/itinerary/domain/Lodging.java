package com.travelplanner.itinerary.domain;

import com.travelplanner.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Hospedagem de uma parada (ver {@link LodgingDetails}). */
@Getter
@Entity
@Table(name = "lodging")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Lodging extends BaseEntity {

  @Column(name = "stop_id", nullable = false, updatable = false)
  private Long stopId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private LodgingKind kind;

  @Column(name = "place_id")
  private String placeId;

  @Column(nullable = false, length = 160)
  private String name;

  @Column(length = 300)
  private String address;

  @Column(name = "check_in", nullable = false)
  private LocalDate checkIn;

  @Column(name = "check_out", nullable = false)
  private LocalDate checkOut;

  @Enumerated(EnumType.STRING)
  @Column(name = "board_type")
  private BoardType boardType;

  @Column(name = "price_tier", columnDefinition = "tinyint")
  private Integer priceTier;

  @Enumerated(EnumType.STRING)
  @Column(name = "price_tier_source")
  private PriceTierSource priceTierSource;

  @Column(name = "booking_ref", length = 80)
  private String bookingRef;

  @Column(columnDefinition = "text")
  private String notes;

  public Lodging(Long stopId, LodgingDetails details) {
    this.stopId = stopId;
    update(details);
  }

  /**
   * Substitui os campos. A faixa de preço informada pelo usuário passa a ter origem {@code USER};
   * se ela não mudou, a origem anterior (ex.: Places) é mantida.
   */
  public void update(LodgingDetails details) {
    this.kind = details.kind();
    this.name = details.name();
    this.address = details.address();
    this.checkIn = details.checkIn();
    this.checkOut = details.checkOut();
    this.boardType = details.boardType();
    if (details.priceTier() == null) {
      this.priceTierSource = null;
    } else if (!Objects.equals(details.priceTier(), priceTier)) {
      this.priceTierSource = PriceTierSource.USER;
    }
    this.priceTier = details.priceTier();
    this.bookingRef = details.bookingRef();
    this.notes = details.notes();
  }

  /**
   * Liga a hospedagem a um lugar do Places (resultado da busca). Se a faixa veio da busca, guarda a
   * origem dela ({@code PLACES} ou {@code HEURISTIC}) no lugar de {@code USER}.
   */
  public void linkPlace(String placeId, PriceTierSource tierSource) {
    this.placeId = placeId;
    if (priceTier != null && tierSource != null) {
      this.priceTierSource = tierSource;
    }
  }

  public LodgingDetails details() {
    return new LodgingDetails(
        kind, name, address, checkIn, checkOut, boardType, priceTier, bookingRef, notes);
  }
}
