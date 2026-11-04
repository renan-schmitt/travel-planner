package com.travelplanner.requirements.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

/** País (dados de referência, só leitura; seed em {@code V16__country.sql}). */
@Getter
@Entity
@Immutable
@Table(name = "country")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Country {

  @Id
  @Column(columnDefinition = "char(2)")
  private String code;

  @Column(name = "name_pt", nullable = false)
  private String namePt;

  @Column(name = "flag_emoji", nullable = false)
  private String flagEmoji;

  @Column(name = "plug_types")
  private String plugTypes;

  private String voltage;

  @Column(columnDefinition = "char(3)")
  private String currency;

  @Column(nullable = false)
  private boolean schengen;

  @Column(name = "br_id_card_accepted", nullable = false)
  private boolean brIdCardAccepted;

  public CountryInfo toInfo() {
    return new CountryInfo(
        code, namePt, flagEmoji, plugTypes, voltage, currency, schengen, brIdCardAccepted);
  }
}
