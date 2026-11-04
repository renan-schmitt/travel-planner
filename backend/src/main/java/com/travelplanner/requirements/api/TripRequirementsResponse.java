package com.travelplanner.requirements.api;

import com.travelplanner.requirements.domain.RequirementType;
import com.travelplanner.requirements.domain.TripRequirementsService.ApplicableRequirement;
import com.travelplanner.requirements.domain.TripRequirementsService.DestinationView;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

/**
 * Documentos de um destino estrangeiro da viagem. {@code country} é nulo se o código não está na
 * base. {@code nationalitiesWithoutData}: nacionalidades dos viajantes sem nenhum requisito
 * publicado para o país (mostrar "sem informações", nunca "não precisa de nada").
 */
public record TripRequirementsResponse(
    @NotNull String countryCode,
    CountryResponse country,
    @NotNull LocalDate arrivalDate,
    @NotNull List<Item> requirements,
    @NotNull List<String> nationalitiesWithoutData) {

  /** {@code travelers} vazio = o dono (viagem sem viajantes cadastrados). */
  public record Item(
      @NotNull Long id,
      @NotNull String nationality,
      @NotNull RequirementType type,
      @NotNull String title,
      String description,
      Integer leadDays,
      @NotNull String officialUrl,
      LocalDate verifiedAt,
      LocalDate dueDate,
      @NotNull List<String> travelers) {

    static Item from(ApplicableRequirement a) {
      var r = a.requirement();
      return new Item(
          r.id(),
          r.nationality(),
          r.type(),
          r.title(),
          r.description(),
          r.leadDays(),
          r.officialUrl(),
          r.verifiedAt(),
          a.dueDate(),
          a.travelers());
    }
  }

  static TripRequirementsResponse from(DestinationView v) {
    return new TripRequirementsResponse(
        v.countryCode(),
        CountryResponse.from(v.country()),
        v.arrival(),
        v.requirements().stream().map(Item::from).toList(),
        v.nationalitiesWithoutData());
  }
}
