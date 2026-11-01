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
}
