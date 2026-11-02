package com.travelplanner.trip.domain;

import com.travelplanner.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Link de partilha: quem o abre (logado) entra na viagem com o papel do link ({@code EDITOR} ou
 * {@code VIEWER}). Revogado, deixa de funcionar.
 */
@Getter
@Entity
@Table(name = "trip_share_link")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TripShareLink extends BaseEntity {

  @Column(name = "trip_id", nullable = false, updatable = false)
  private Long tripId;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(nullable = false, updatable = false, length = 43)
  private String token;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, updatable = false)
  private TripRole role;

  @Column(name = "created_by", nullable = false, updatable = false)
  private Long createdBy;

  @Column(name = "revoked_at")
  private Instant revokedAt;

  public TripShareLink(long tripId, String token, TripRole role, long createdBy) {
    if (role == TripRole.OWNER) {
      throw new IllegalArgumentException("Link de partilha não pode dar papel de dono.");
    }
    this.tripId = tripId;
    this.token = token;
    this.role = role;
    this.createdBy = createdBy;
  }

  public boolean isRevoked() {
    return revokedAt != null;
  }

  public void revoke(Instant now) {
    if (revokedAt == null) {
      revokedAt = now;
    }
  }
}
