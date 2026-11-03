package com.travelplanner.suggestion.domain.rules;

import com.travelplanner.itinerary.domain.TripSnapshot.SegmentEntry;
import com.travelplanner.suggestion.domain.GapContext;
import com.travelplanner.suggestion.domain.GapRule;
import com.travelplanner.suggestion.domain.Severity;
import com.travelplanner.suggestion.domain.SuggestionDraft;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * {@code TRANSFER_CHEGADA}: voo que chega a uma parada sem trecho de chão saindo do aeroporto até 6
 * h depois (trecho local da parada, ou que sai da parada de destino do voo).
 */
@Component
public class TransferArrivalRule implements GapRule {

  public static final String CODE = "TRANSFER_CHEGADA";
  static final Duration WINDOW = Duration.ofHours(6);

  @Override
  public String code() {
    return CODE;
  }

  @Override
  public List<SuggestionDraft> evaluate(GapContext context) {
    List<SegmentEntry> segments = context.snapshot().segments();
    List<SuggestionDraft> drafts = new ArrayList<>();
    for (SegmentEntry flight : segments) {
      Long stopId = flight.details().destinationStopId();
      if (!RuleSupport.flight(flight) || stopId == null) {
        continue;
      }
      Instant arrival = RuleSupport.arrivalOrDeparture(flight);
      boolean covered =
          segments.stream()
              .filter(RuleSupport::ground)
              .filter(
                  g ->
                      Objects.equals(g.details().localStopId(), stopId)
                          || Objects.equals(g.details().originStopId(), stopId))
              .anyMatch(
                  g ->
                      RuleSupport.between(RuleSupport.departure(g), arrival, arrival.plus(WINDOW)));
      if (!covered) {
        Map<String, Object> params = RuleSupport.segmentParams(context, flight);
        params.put("stopId", stopId);
        params.put("city", context.cityOf(stopId));
        drafts.add(SuggestionDraft.of(String.valueOf(flight.id()), Severity.WARNING, params));
      }
    }
    return drafts;
  }
}
