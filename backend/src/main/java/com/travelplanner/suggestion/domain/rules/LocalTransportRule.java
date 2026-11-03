package com.travelplanner.suggestion.domain.rules;

import com.travelplanner.itinerary.domain.TransportType;
import com.travelplanner.itinerary.domain.TripSnapshot.StopEntry;
import com.travelplanner.suggestion.domain.GapContext;
import com.travelplanner.suggestion.domain.GapRule;
import com.travelplanner.suggestion.domain.Severity;
import com.travelplanner.suggestion.domain.SuggestionDraft;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * {@code TRANSPORTE_LOCAL}: parada com 2 noites ou mais sem aluguel de carro (trecho local {@code
 * CAR_RENTAL}). Só sugere decidir como se locomover; dispensar resolve.
 */
@Component
public class LocalTransportRule implements GapRule {

  public static final String CODE = "TRANSPORTE_LOCAL";
  static final int MIN_NIGHTS = 2;

  @Override
  public String code() {
    return CODE;
  }

  @Override
  public List<SuggestionDraft> evaluate(GapContext context) {
    return context.snapshot().stops().stream()
        .filter(stop -> nights(stop) >= MIN_NIGHTS)
        .filter(stop -> !hasCarRental(context, stop))
        .map(
            stop ->
                SuggestionDraft.of(
                    String.valueOf(stop.id()),
                    Severity.INFO,
                    Map.of(
                        "stopId", stop.id(),
                        "city", stop.details().cityName(),
                        "nights", nights(stop))))
        .toList();
  }

  private static long nights(StopEntry stop) {
    return ChronoUnit.DAYS.between(stop.details().arrivalDate(), stop.details().departureDate());
  }

  private static boolean hasCarRental(GapContext context, StopEntry stop) {
    return context.snapshot().segments().stream()
        .anyMatch(
            s ->
                s.details().type() == TransportType.CAR_RENTAL
                    && Objects.equals(s.details().localStopId(), stop.id()));
  }
}
