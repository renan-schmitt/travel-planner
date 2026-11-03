package com.travelplanner.suggestion.domain.rules;

import com.travelplanner.itinerary.domain.ItineraryBuilder;
import com.travelplanner.itinerary.domain.ItineraryDay;
import com.travelplanner.itinerary.domain.TripSnapshot.StopEntry;
import com.travelplanner.suggestion.domain.GapContext;
import com.travelplanner.suggestion.domain.GapRule;
import com.travelplanner.suggestion.domain.Severity;
import com.travelplanner.suggestion.domain.SuggestionDraft;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * {@code NOITE_SEM_HOSPEDAGEM}: noite entre a primeira chegada e a última saída das paradas sem
 * hospedagem cobrindo e sem trecho em andamento. Usa a mesma definição de noite da timeline ({@link
 * ItineraryBuilder}, noite {@code MISSING}), para a timeline e as sugestões nunca discordarem;
 * noites em casa (antes da ida/depois da volta) não contam.
 */
@Component
public class NightWithoutLodgingRule implements GapRule {

  public static final String CODE = "NOITE_SEM_HOSPEDAGEM";

  @Override
  public String code() {
    return CODE;
  }

  @Override
  public List<SuggestionDraft> evaluate(GapContext context) {
    return ItineraryBuilder.build(context.snapshot()).stream()
        .filter(ItineraryDay::gap)
        .map(day -> draft(context, day.date()))
        .toList();
  }

  private static SuggestionDraft draft(GapContext context, LocalDate night) {
    Map<String, Object> params = new LinkedHashMap<>();
    params.put("date", night.toString());
    stopOfNight(context, night)
        .ifPresent(
            stop -> {
              params.put("stopId", stop.id());
              params.put("city", stop.details().cityName());
            });
    return SuggestionDraft.of(night.toString(), Severity.WARNING, params);
  }

  /** A parada em que se dorme naquela noite ({@code chegada <= noite < saída}). */
  private static Optional<StopEntry> stopOfNight(GapContext context, LocalDate night) {
    return context.snapshot().stops().stream()
        .filter(
            s ->
                !night.isBefore(s.details().arrivalDate())
                    && night.isBefore(s.details().departureDate()))
        .min(Comparator.comparingInt(StopEntry::seq));
  }
}
