package com.travelplanner.auth.domain;

import com.travelplanner.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Usuário do sistema, identificado pelo {@code sub} do Google. */
@Getter
@Entity
@Table(name = "app_user")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppUser extends BaseEntity {

  public static final String DEFAULT_LOCALE = "pt-BR";
  public static final String DEFAULT_NATIONALITY = "BR";

  @Column(name = "google_sub", nullable = false, updatable = false)
  private String googleSub;

  @Column(nullable = false, length = 320)
  private String email;

  private String name;

  @Column(name = "picture_url", length = 1024)
  private String pictureUrl;

  @Column(nullable = false, length = 16)
  private String locale = DEFAULT_LOCALE;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(nullable = false, length = 2)
  private String nationality = DEFAULT_NATIONALITY;

  @Column(name = "home_city_name")
  private String homeCityName;

  @Column(name = "home_city_place_id")
  private String homeCityPlaceId;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "home_country_code", length = 2)
  private String homeCountryCode;

  @Column(name = "home_timezone", length = 64)
  private String homeTimezone;

  public AppUser(String googleSub, String email, String name, String pictureUrl) {
    this.googleSub = googleSub;
    updateIdentity(email, name, pictureUrl);
  }

  /** Dados que vêm do Google a cada login. */
  public void updateIdentity(String email, String name, String pictureUrl) {
    this.email = email;
    this.name = name;
    this.pictureUrl = pictureUrl;
  }

  /** Dados editados pelo próprio usuário no perfil. {@code homeCity} nulo limpa a cidade. */
  public void updateProfile(String nationality, HomeCity homeCity) {
    this.nationality = nationality;
    this.homeCityName = homeCity == null ? null : homeCity.name();
    this.homeCityPlaceId = homeCity == null ? null : homeCity.placeId();
    this.homeCountryCode = homeCity == null ? null : homeCity.countryCode();
    this.homeTimezone = homeCity == null ? null : homeCity.timezone();
  }

  /** Cidade de origem, ou {@code null} se ainda não foi preenchida. */
  public HomeCity getHomeCity() {
    if (homeCityName == null) {
      return null;
    }
    return new HomeCity(homeCityName, homeCityPlaceId, homeCountryCode, homeTimezone);
  }
}
