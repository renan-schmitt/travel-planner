package com.travelplanner.suggestion.domain.rules;

import com.travelplanner.itinerary.domain.SavedPlaceKind;
import org.springframework.stereotype.Component;

/** {@code ATRACOES}: parada com 1 noite ou mais e nenhuma atração favorita. */
@Component
public class AttractionsRule extends MissingFavoritesRule {

  public static final String CODE = "ATRACOES";

  public AttractionsRule() {
    super(SavedPlaceKind.ATTRACTION, 1);
  }

  @Override
  public String code() {
    return CODE;
  }
}
