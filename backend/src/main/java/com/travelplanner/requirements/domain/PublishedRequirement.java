package com.travelplanner.requirements.domain;

import java.time.LocalDate;

/** Requisito publicado, como as regras e a tela da viagem o veem. */
public record PublishedRequirement(
    long id,
    String countryCode,
    String nationality,
    RequirementType type,
    String title,
    String description,
    Integer leadDays,
    String officialUrl,
    LocalDate verifiedAt) {

  /** Antecedência padrão do visto quando o registro não informa {@code leadDays}. */
  public static final int VISA_DEFAULT_LEAD_DAYS = 30;

  /**
   * Data limite para resolver o requisito, contando da chegada ao país: chegada − {@code leadDays};
   * visto sem {@code leadDays}, chegada − 30 dias; os demais sem {@code leadDays}, sem data.
   */
  public LocalDate dueDate(LocalDate arrival) {
    if (arrival == null) {
      return null;
    }
    if (leadDays != null) {
      return arrival.minusDays(leadDays);
    }
    return type == RequirementType.VISA ? arrival.minusDays(VISA_DEFAULT_LEAD_DAYS) : null;
  }
}
