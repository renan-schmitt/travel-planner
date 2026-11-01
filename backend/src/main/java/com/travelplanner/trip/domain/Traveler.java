package com.travelplanner.trip.domain;

import com.travelplanner.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Pessoa que viaja, com ou sem conta. Pertence a um usuário ({@code ownerUserId}) e é reaproveitada
 * entre viagens. O viajante "eu" de cada usuário tem {@code linkedUserId} igual ao dono. Não guarda
 * o número do passaporte, só a validade.
 */
@Getter
@Entity
@Table(name = "traveler")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Traveler extends BaseEntity {

  @Column(name = "owner_user_id", nullable = false, updatable = false)
  private Long ownerUserId;

  @Column(name = "linked_user_id", updatable = false)
  private Long linkedUserId;

  @Column(nullable = false, length = 120)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(name = "age_group", nullable = false)
  private AgeGroup ageGroup;

  @JdbcTypeCode(SqlTypes.SMALLINT)
  @Column(name = "birth_year")
  private Integer birthYear;

  /** Códigos ISO separados por vírgula ({@code "BR,PT"}). */
  @Column(name = "nationalities", nullable = false, length = 64)
  private String nationalityCodes;

  @Column(name = "passport_expiry")
  private LocalDate passportExpiry;

  public Traveler(Long ownerUserId, TravelerDetails details) {
    this.ownerUserId = ownerUserId;
    update(details);
  }

  /** O viajante "eu" do usuário, criado no primeiro login. */
  public static Traveler self(long userId, String name, String nationality) {
    Traveler traveler =
        new Traveler(
            userId, new TravelerDetails(name, AgeGroup.ADULT, null, List.of(nationality), null));
    traveler.linkedUserId = userId;
    return traveler;
  }

  public void update(TravelerDetails details) {
    this.name = details.name();
    this.ageGroup = details.ageGroup();
    this.birthYear = details.birthYear();
    this.nationalityCodes = String.join(",", details.nationalities());
    this.passportExpiry = details.passportExpiry();
  }

  public List<String> getNationalities() {
    return Arrays.stream(nationalityCodes.split(",")).filter(s -> !s.isBlank()).toList();
  }

  public boolean isOwnedBy(long userId) {
    return ownerUserId == userId;
  }

  /** {@code true} se é o viajante "eu" do dono (vinculado à própria conta). */
  public boolean isSelf() {
    return Objects.equals(linkedUserId, ownerUserId);
  }
}
