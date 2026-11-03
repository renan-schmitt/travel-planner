package com.travelplanner.suggestion.domain.rules;

import com.travelplanner.itinerary.domain.TripSnapshot.StopEntry;
import com.travelplanner.suggestion.domain.GapContext;
import com.travelplanner.suggestion.domain.GapRule;
import com.travelplanner.suggestion.domain.Severity;
import com.travelplanner.suggestion.domain.SuggestionDraft;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * {@code DESTINO_SEM_TRANSPORTE}: para cada par consecutivo do roteiro (casa → 1ª parada, parada i
 * → i+1, última → casa), nenhum trecho com essa origem e esse destino. Chave: {@code home-<id>},
 * {@code <id>-<id>} ou {@code <id>-home}.
 */
@Component
public class MissingTransportRule implements GapRule {

  public static final String CODE = "DESTINO_SEM_TRANSPORTE";

  @Override
  public String code() {
    return CODE;
  }

  @Override
  public List<SuggestionDraft> evaluate(GapContext context) {
    List<StopEntry> stops =
        context.snapshot().stops().stream()
            .sorted(Comparator.comparingInt(StopEntry::seq))
            .toList();
    if (stops.isEmpty()) {
      return List.of();
    }
    List<Long> path = new ArrayList<>();
    path.add(null);
    stops.forEach(s -> path.add(s.id()));
    path.add(null);
    List<SuggestionDraft> drafts = new ArrayList<>();
    for (int i = 0; i + 1 < path.size(); i++) {
      Long from = path.get(i);
      Long to = path.get(i + 1);
      boolean covered =
          context.snapshot().segments().stream()
              .filter(s -> !s.details().local())
              .anyMatch(
                  s ->
                      Objects.equals(s.details().originStopId(), from)
                          && Objects.equals(s.details().destinationStopId(), to));
      if (!covered) {
        drafts.add(draft(context, from, to));
      }
    }
    return drafts;
  }

  private static SuggestionDraft draft(GapContext context, Long from, Long to) {
    Map<String, Object> params = new LinkedHashMap<>();
    RuleSupport.putIfPresent(params, "fromStopId", from);
    RuleSupport.putIfPresent(params, "fromCity", context.cityOf(from));
    RuleSupport.putIfPresent(params, "toStopId", to);
    RuleSupport.putIfPresent(params, "toCity", context.cityOf(to));
    params.put("fromHome", from == null);
    params.put("toHome", to == null);
    return SuggestionDraft.of(key(from) + "-" + key(to), Severity.WARNING, params);
  }

  private static String key(Long stopId) {
    return stopId == null ? "home" : stopId.toString();
  }
}
