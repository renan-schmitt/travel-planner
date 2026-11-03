package com.travelplanner.suggestion.domain.rules;

import com.travelplanner.checklist.domain.ChecklistCategory;
import com.travelplanner.itinerary.domain.TripSnapshot.SegmentEntry;
import com.travelplanner.suggestion.domain.ChecklistDraft;
import com.travelplanner.suggestion.domain.GapContext;
import com.travelplanner.suggestion.domain.GapRule;
import com.travelplanner.suggestion.domain.Severity;
import com.travelplanner.suggestion.domain.SuggestionDraft;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * {@code CHECKIN_ONLINE}: voo que parte nos próximos 3 dias (pelo {@code Clock}). Cria na hora o
 * item "Check-in online &lt;voo&gt;", com prazo no dia anterior à partida (data local).
 */
@Component
public class OnlineCheckinRule implements GapRule {

  public static final String CODE = "CHECKIN_ONLINE";
  static final Duration WINDOW = Duration.ofDays(3);

  @Override
  public String code() {
    return CODE;
  }

  @Override
  public List<SuggestionDraft> evaluate(GapContext context) {
    Instant now = context.now();
    return context.snapshot().segments().stream()
        .filter(RuleSupport::flight)
        .filter(s -> RuleSupport.departure(s).isAfter(now))
        .filter(s -> !RuleSupport.departure(s).isAfter(now.plus(WINDOW)))
        .map(s -> draft(context, s))
        .toList();
  }

  private static SuggestionDraft draft(GapContext context, SegmentEntry flight) {
    String label = RuleSupport.flightLabel(context, flight);
    var params = RuleSupport.segmentParams(context, flight);
    params.put("flight", label);
    return SuggestionDraft.of(String.valueOf(flight.id()), Severity.INFO, params)
        .withChecklist(
            new ChecklistDraft(
                ChecklistCategory.BOOKINGS,
                "Check-in online " + label,
                flight.details().departAt().toLocalDate().minusDays(1),
                true));
  }
}
