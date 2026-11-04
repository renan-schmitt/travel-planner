package com.travelplanner.requirements.domain;

import com.travelplanner.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Requisito de entrada num país para uma nacionalidade. Só vale para as viagens depois de
 * publicado.
 */
@Getter
@Entity
@Table(name = "destination_requirement")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DestinationRequirement extends BaseEntity {

  @Column(name = "country_code", nullable = false, columnDefinition = "char(2)")
  private String countryCode;

  @Column(nullable = false, columnDefinition = "char(2)")
  private String nationality;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private RequirementType type;

  @Column(nullable = false, length = 200)
  private String title;

  @Column(columnDefinition = "text")
  private String description;

  @Column(name = "lead_days")
  private Integer leadDays;

  @Column(name = "official_url", nullable = false, length = 500)
  private String officialUrl;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private RequirementStatus status;

  @Column(name = "verified_at")
  private LocalDate verifiedAt;

  @Column(name = "verified_by")
  private Long verifiedBy;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private RequirementSource source;

  public DestinationRequirement(RequirementDetails details, RequirementSource source) {
    apply(details);
    this.status = RequirementStatus.DRAFT;
    this.source = source;
  }

  public final void apply(RequirementDetails details) {
    this.countryCode = details.countryCode();
    this.nationality = details.nationality();
    this.type = details.type();
    this.title = details.title();
    this.description = details.description();
    this.leadDays = details.leadDays();
    this.officialUrl = details.officialUrl();
  }

  /** Publica (ou confirma de novo): passa a valer e registra quem verificou e quando. */
  public void publish(long userId, LocalDate today) {
    this.status = RequirementStatus.PUBLISHED;
    this.verifiedAt = today;
    this.verifiedBy = userId;
  }

  /** Volta para rascunho (deixa de valer); a última verificação fica registrada. */
  public void unpublish() {
    this.status = RequirementStatus.DRAFT;
  }

  public PublishedRequirement toPublished() {
    return new PublishedRequirement(
        getId(),
        countryCode,
        nationality,
        type,
        title,
        description,
        leadDays,
        officialUrl,
        verifiedAt);
  }
}
