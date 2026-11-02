package com.travelplanner.trip.domain;

import com.travelplanner.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Membro de uma viagem com um papel. Convites pendentes (Fase 11) têm só {@code inviteEmail}; ao
 * aceitar, {@code userId} é preenchido.
 */
@Getter
@Entity
@Table(name = "trip_member")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TripMember extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "trip_id", nullable = false, updatable = false)
  private Trip trip;

  @Column(name = "user_id")
  private Long userId;

  @Column(name = "invite_email", length = 320)
  private String inviteEmail;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TripRole role;

  @Column(name = "accepted_at")
  private Instant acceptedAt;

  /** Membro que já tem conta (o dono, ou um convite aceito). */
  public static TripMember of(Trip trip, Long userId, TripRole role, Instant acceptedAt) {
    TripMember member = new TripMember();
    member.trip = trip;
    member.userId = userId;
    member.role = role;
    member.acceptedAt = acceptedAt;
    return member;
  }

  /** Convite pendente: só o e-mail, até a pessoa entrar com ele. */
  public static TripMember invite(Trip trip, String email, TripRole role) {
    TripMember member = new TripMember();
    member.trip = trip;
    member.inviteEmail = email;
    member.role = role;
    return member;
  }

  /** {@code true} enquanto o convidado não entrou no sistema com o e-mail do convite. */
  public boolean isPending() {
    return userId == null;
  }

  /** Vincula o convite pendente ao usuário que entrou com o e-mail. */
  public void accept(long userId, Instant now) {
    this.userId = userId;
    this.acceptedAt = now;
  }

  public void changeRole(TripRole role) {
    this.role = role;
  }
}
