package com.travelplanner.suggestion.domain.rules;

import com.travelplanner.checklist.domain.ChecklistCategory;
import com.travelplanner.itinerary.domain.BoardType;
import com.travelplanner.itinerary.domain.LodgingDetails;
import com.travelplanner.itinerary.domain.LodgingKind;
import com.travelplanner.itinerary.domain.TripSnapshot.LodgingEntry;
import com.travelplanner.suggestion.domain.ChecklistDraft;
import com.travelplanner.suggestion.domain.GapContext;
import com.travelplanner.suggestion.domain.GapRule;
import com.travelplanner.suggestion.domain.Severity;
import com.travelplanner.suggestion.domain.SuggestionDraft;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * {@code RESORT_OPCOES}: hospedagem all-inclusive ou com "resort" no nome (casa de amigo não
 * conta). Resorts costumam exigir reserva nos restaurantes à la carte: aceitar cria o item "Ver
 * restaurantes à la carte/reservas do resort", com prazo na véspera do check-in. {@code placeId}
 * vai nos parâmetros para o frontend abrir a página do lugar.
 */
@Component
public class ResortOptionsRule implements GapRule {

  public static final String CODE = "RESORT_OPCOES";

  @Override
  public String code() {
    return CODE;
  }

  @Override
  public List<SuggestionDraft> evaluate(GapContext context) {
    return context.snapshot().lodgings().stream()
        .filter(ResortOptionsRule::resort)
        .map(lodging -> draft(context, lodging))
        .toList();
  }

  static boolean resort(LodgingEntry lodging) {
    LodgingDetails d = lodging.details();
    if (d.kind() == LodgingKind.FRIENDS_HOME) {
      return false;
    }
    return d.boardType() == BoardType.ALL_INCLUSIVE
        || GapContext.normalize(d.name()).contains("resort");
  }

  private static SuggestionDraft draft(GapContext context, LodgingEntry lodging) {
    LodgingDetails d = lodging.details();
    Map<String, Object> params = new LinkedHashMap<>();
    params.put("lodgingId", lodging.id());
    params.put("name", d.name());
    params.put("stopId", lodging.stopId());
    RuleSupport.putIfPresent(params, "city", context.cityOf(lodging.stopId()));
    RuleSupport.putIfPresent(params, "placeId", lodging.placeId());
    params.put("allInclusive", d.boardType() == BoardType.ALL_INCLUSIVE);
    return SuggestionDraft.of(String.valueOf(lodging.id()), Severity.INFO, params)
        .withChecklist(
            new ChecklistDraft(
                ChecklistCategory.BOOKINGS,
                "Ver restaurantes à la carte/reservas do resort: " + d.name(),
                d.checkIn().minusDays(1),
                false));
  }
}
