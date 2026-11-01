package com.travelplanner.itinerary.domain;

import com.travelplanner.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Parada (destino) da viagem. {@code seq} define a ordem do roteiro (único por viagem). */
@Getter
@Entity
@Table(name = "stop")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Stop extends BaseEntity {

  @Column(name = "trip_id", nullable = false, updatable = false)
  private Long tripId;

  @Column(nullable = false)
  private int seq;

  @Column(name = "city_place_id")
  private String cityPlaceId;

  @Column(name = "city_name", nullable = false, length = 160)
  private String cityName;

  @Column(name = "country_code", nullable = false, length = 2, columnDefinition = "char(2)")
  private String countryCode;

  @Column(precision = 9, scale = 6)
  private BigDecimal lat;

  @Column(precision = 9, scale = 6)
  private BigDecimal lng;

  @Column(nullable = false, length = 64)
  private String timezone;

  @Column(name = "arrival_date", nullable = false)
  private LocalDate arrivalDate;

  @Column(name = "departure_date", nullable = false)
  private LocalDate departureDate;

  @Column(columnDefinition = "text")
  private String notes;

  public Stop(Long tripId, StopDetails details) {
    this.tripId = tripId;
    update(details);
  }

  public void update(StopDetails details) {
    this.cityPlaceId = details.cityPlaceId();
    this.cityName = details.cityName();
    this.countryCode = details.countryCode();
    this.lat = details.lat();
    this.lng = details.lng();
    this.timezone = details.timezone();
    this.arrivalDate = details.arrivalDate();
    this.departureDate = details.departureDate();
    this.notes = details.notes();
  }

  void moveTo(int seq) {
    this.seq = seq;
  }

  /** Noites na cidade (0 para um bate-volta). */
  public int nights() {
    return (int) ChronoUnit.DAYS.between(arrivalDate, departureDate);
  }
}
