package com.travelplanner.requirements.api;

import com.travelplanner.requirements.domain.DestinationRequirement;
import com.travelplanner.requirements.domain.RequirementSource;
import com.travelplanner.requirements.domain.RequirementStatus;
import com.travelplanner.requirements.domain.RequirementType;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/** Requisito na tela de administração. */
public record RequirementResponse(
    @NotNull Long id,
    @NotNull String countryCode,
    @NotNull String nationality,
    @NotNull RequirementType type,
    @NotNull String title,
    String description,
    Integer leadDays,
    @NotNull String officialUrl,
    @NotNull RequirementStatus status,
    LocalDate verifiedAt,
    @NotNull RequirementSource source,
    @NotNull Long version) {

  static RequirementResponse from(DestinationRequirement r) {
    return new RequirementResponse(
        r.getId(),
        r.getCountryCode(),
        r.getNationality(),
        r.getType(),
        r.getTitle(),
        r.getDescription(),
        r.getLeadDays(),
        r.getOfficialUrl(),
        r.getStatus(),
        r.getVerifiedAt(),
        r.getSource(),
        r.getVersion());
  }
}
