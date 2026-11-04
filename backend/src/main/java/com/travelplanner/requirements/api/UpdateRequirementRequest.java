package com.travelplanner.requirements.api;

import com.travelplanner.requirements.domain.RequirementDetails;
import com.travelplanner.requirements.domain.RequirementType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Edição completa do requisito, com a {@code version} lida (409 se divergente). */
public record UpdateRequirementRequest(
    @NotNull @Pattern(regexp = "[A-Za-z]{2}") String countryCode,
    @NotNull @Pattern(regexp = "[A-Za-z]{2}") String nationality,
    @NotNull RequirementType type,
    @NotBlank @Size(max = 200) String title,
    @Size(max = 4000) String description,
    @Min(0) @Max(365) Integer leadDays,
    @NotBlank @Size(max = 500) @Pattern(regexp = "https?://\\S+") String officialUrl,
    @NotNull Long version) {

  RequirementDetails toDomain() {
    return new RequirementDetails(
        countryCode, nationality, type, title, description, leadDays, officialUrl);
  }
}
