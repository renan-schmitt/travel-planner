package com.travelplanner.trip.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.Serializable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Viajante que vai numa viagem (tabela de associação, sem auditoria). */
@Getter
@Entity
@Table(name = "trip_traveler")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TripTraveler {

  @EmbeddedId private Key id;

  public TripTraveler(long tripId, long travelerId) {
    this.id = new Key(tripId, travelerId);
  }

  @Embeddable
  public record Key(
      @Column(name = "trip_id", nullable = false) Long tripId,
      @Column(name = "traveler_id", nullable = false) Long travelerId)
      implements Serializable {}
}
