package com.travelplanner.suggestion.domain;

import com.travelplanner.checklist.domain.ChecklistStatus;
import com.travelplanner.itinerary.domain.TripSnapshot;
import com.travelplanner.itinerary.domain.TripSnapshot.StopEntry;
import java.text.Normalizer;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Entrada das regras: o {@link TripSnapshot}, o relógio e o resumo do checklist da viagem (dado
 * fora do snapshot, para regras como {@code SEGURO_VIAGEM}). Imutável.
 */
public record GapContext(TripSnapshot snapshot, Clock clock, List<ChecklistEntry> checklist) {

  /** País usado quando o dono não tem cidade de origem nem nacionalidade. */
  public static final String DEFAULT_HOME_COUNTRY = "BR";

  public GapContext {
    checklist = List.copyOf(checklist);
  }

  /** Item do checklist visto pelas regras. */
  public record ChecklistEntry(
      String ruleCode, String ruleKey, String title, ChecklistStatus status) {}

  public Instant now() {
    return clock.instant();
  }

  /** País de casa: o da cidade de origem do dono, senão a nacionalidade dele, senão Brasil. */
  public String homeCountry() {
    TripSnapshot.Owner owner = snapshot.owner();
    if (owner.homeCity() != null && owner.homeCity().countryCode() != null) {
      return owner.homeCity().countryCode();
    }
    return owner.nationality() != null ? owner.nationality() : DEFAULT_HOME_COUNTRY;
  }

  /** Países das paradas diferentes do país de casa. Vazio = viagem nacional. */
  public Set<String> foreignCountries() {
    String home = homeCountry();
    return snapshot.stops().stream()
        .map(s -> s.details().countryCode())
        .filter(Objects::nonNull)
        .filter(c -> !c.equalsIgnoreCase(home))
        .map(c -> c.toUpperCase(Locale.ROOT))
        .collect(Collectors.toCollection(TreeSet::new));
  }

  public boolean international() {
    return !foreignCountries().isEmpty();
  }

  /** Nome da cidade da parada, ou {@code null} para "casa". */
  public String cityOf(Long stopId) {
    return snapshot.stop(stopId).map(StopEntry::details).map(d -> d.cityName()).orElse(null);
  }

  /** Texto em minúsculas, sem acentos e com espaços simples (para comparar títulos). */
  public static String normalize(String text) {
    return Normalizer.normalize(text, Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .toLowerCase(Locale.ROOT)
        .replaceAll("\\s+", " ")
        .trim();
  }
}
