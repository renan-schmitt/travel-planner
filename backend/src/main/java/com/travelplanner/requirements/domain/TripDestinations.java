package com.travelplanner.requirements.domain;

import com.travelplanner.itinerary.domain.TripSnapshot;
import com.travelplanner.itinerary.domain.TripSnapshot.StopEntry;
import com.travelplanner.itinerary.domain.TripSnapshot.TravelerEntry;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Destinos estrangeiros da viagem e quem precisa de documentos para cada um (lógica pura, usada
 * pelas regras e pela tela de documentos).
 *
 * <ul>
 *   <li>País de casa: o da cidade de origem do dono, senão a nacionalidade dele, senão Brasil.
 *   <li>Destino: país de alguma parada diferente do de casa; chegada = a primeira chegada nele.
 *   <li>Cada viajante conta pela nacionalidade <b>principal</b> (a primeira); quem tem a
 *       nacionalidade do destino (qualquer uma) fica de fora. Sem viajantes, conta a nacionalidade
 *       do dono.
 * </ul>
 */
public final class TripDestinations {

  public static final String DEFAULT_HOME_COUNTRY = "BR";

  private TripDestinations() {}

  /**
   * País estrangeiro da viagem. {@code travelersByNationality}: nacionalidade principal → nomes
   * (lista vazia = o dono, viagem sem viajantes); vazio = todos são cidadãos do país.
   */
  public record Destination(
      String countryCode, LocalDate arrival, Map<String, List<String>> travelersByNationality) {}

  public static String homeCountry(TripSnapshot snapshot) {
    TripSnapshot.Owner owner = snapshot.owner();
    if (owner.homeCity() != null && owner.homeCity().countryCode() != null) {
      return owner.homeCity().countryCode().toUpperCase(Locale.ROOT);
    }
    return owner.nationality() != null
        ? owner.nationality().toUpperCase(Locale.ROOT)
        : DEFAULT_HOME_COUNTRY;
  }

  /** Em ordem de código do país. */
  public static List<Destination> of(TripSnapshot snapshot) {
    String home = homeCountry(snapshot);
    Map<String, LocalDate> arrivals = new TreeMap<>();
    for (StopEntry stop : snapshot.stops()) {
      String code = stop.details().countryCode();
      if (code == null || code.equalsIgnoreCase(home)) {
        continue;
      }
      arrivals.merge(
          code.toUpperCase(Locale.ROOT),
          stop.details().arrivalDate(),
          (a, b) -> a.isBefore(b) ? a : b);
    }
    List<Destination> result = new ArrayList<>();
    arrivals.forEach(
        (code, arrival) -> result.add(new Destination(code, arrival, travelers(snapshot, code))));
    return result;
  }

  private static Map<String, List<String>> travelers(TripSnapshot snapshot, String country) {
    Map<String, List<String>> byNationality = new LinkedHashMap<>();
    if (snapshot.travelers().isEmpty()) {
      String nationality =
          Objects.requireNonNullElse(snapshot.owner().nationality(), DEFAULT_HOME_COUNTRY)
              .toUpperCase(Locale.ROOT);
      if (!nationality.equals(country)) {
        byNationality.put(nationality, List.of());
      }
      return byNationality;
    }
    for (TravelerEntry traveler : snapshot.travelers()) {
      List<String> nationalities =
          traveler.details().nationalities().stream().map(n -> n.toUpperCase(Locale.ROOT)).toList();
      if (nationalities.contains(country)) {
        continue;
      }
      byNationality
          .computeIfAbsent(nationalities.getFirst(), n -> new ArrayList<>())
          .add(traveler.details().name());
    }
    return byNationality;
  }
}
