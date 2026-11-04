package com.travelplanner.requirements.domain;

import com.travelplanner.itinerary.domain.TripSnapshot;
import com.travelplanner.itinerary.domain.TripSnapshotLoader;
import com.travelplanner.requirements.domain.TripDestinations.Destination;
import com.travelplanner.trip.domain.TripAccessService;
import com.travelplanner.trip.domain.TripRole;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Documentos da viagem: requisitos publicados por destino estrangeiro × nacionalidade. */
@Service
@RequiredArgsConstructor
public class TripRequirementsService {

  private final TripAccessService access;
  private final TripSnapshotLoader snapshots;
  private final RequirementCatalog catalog;

  /** Requisito que vale para a viagem, com a data limite e quem precisa dele. */
  public record ApplicableRequirement(
      PublishedRequirement requirement, LocalDate dueDate, List<String> travelers) {}

  /**
   * Um destino: dados do país (nulo se o código não está na base), requisitos que valem e as
   * nacionalidades sem nenhum requisito publicado ("sem informações").
   */
  public record DestinationView(
      String countryCode,
      CountryInfo country,
      LocalDate arrival,
      List<ApplicableRequirement> requirements,
      List<String> nationalitiesWithoutData) {}

  @Transactional(readOnly = true)
  public List<DestinationView> forTrip(long tripId, long userId) {
    access.requireRole(tripId, userId, TripRole.VIEWER);
    TripSnapshot snapshot = snapshots.load(tripId);
    List<Destination> destinations = TripDestinations.of(snapshot);
    RequirementsData data =
        catalog.load(destinations.stream().map(Destination::countryCode).toList());
    return destinations.stream().map(d -> view(d, data)).toList();
  }

  /** Cruza um destino com a base (também usado pelas regras de documentos). */
  public static DestinationView view(Destination destination, RequirementsData data) {
    List<ApplicableRequirement> applicable = new ArrayList<>();
    List<String> withoutData = new ArrayList<>();
    destination
        .travelersByNationality()
        .forEach(
            (nationality, travelers) -> {
              List<PublishedRequirement> found =
                  data.forCountry(destination.countryCode(), nationality);
              if (found.isEmpty()) {
                withoutData.add(nationality);
              }
              found.forEach(
                  r ->
                      applicable.add(
                          new ApplicableRequirement(
                              r, r.dueDate(destination.arrival()), travelers)));
            });
    return new DestinationView(
        destination.countryCode(),
        data.country(destination.countryCode()).orElse(null),
        destination.arrival(),
        applicable,
        withoutData);
  }
}
