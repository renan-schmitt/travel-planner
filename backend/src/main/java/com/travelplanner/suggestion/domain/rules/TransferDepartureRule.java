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
 * {@code TRANSFER_SAIDA}: voo sem trecho de chão chegando ao aeroporto de origem nas 8 h antes da
 * partida (trecho local da parada de origem, ou que termina nela). Voo saindo de casa: o roteiro
 * não tem como representar a ida de casa ao aeroporto, então a lacuna só some ao dispensar ou
 * aceitar.
 */
@Component
public class TransferDepartureRule implements GapRule {

  public static final String CODE = "TRANSFER_SAIDA";
  static final Duration WINDOW = Duration.ofHours(8);

  @Override
  public String code() {
    return CODE;
  }

  @Override
  public List<SuggestionDraft> evaluate(GapContext context) {
    List<SegmentEntry> segments = context.snapshot().segments();
    List<SuggestionDraft> drafts = new ArrayList<>();
    for (SegmentEntry flight : segments) {
      if (!RuleSupport.flight(flight)) {
        continue;
      }
      Long stopId = flight.details().originStopId();
      Instant departure = RuleSupport.departure(flight);
      boolean covered =
          stopId != null
              && segments.stream()
                  .filter(RuleSupport::ground)
                  .filter(
                      g ->
                          Objects.equals(g.details().localStopId(), stopId)
                              || Objects.equals(g.details().destinationStopId(), stopId))
                  .anyMatch(
                      g ->
                          RuleSupport.between(
                              RuleSupport.arrivalOrDeparture(g),
                              departure.minus(WINDOW),
                              departure));
      if (!covered) {
        Map<String, Object> params = RuleSupport.segmentParams(context, flight);
        RuleSupport.putIfPresent(params, "stopId", stopId);
        RuleSupport.putIfPresent(params, "city", context.cityOf(stopId));
        params.put("fromHome", stopId == null);
        drafts.add(SuggestionDraft.of(String.valueOf(flight.id()), Severity.WARNING, params));
      }
    }
    return drafts;
  }
}
