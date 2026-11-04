package com.travelplanner.suggestion.domain.rules;

import com.travelplanner.itinerary.domain.SavedPlaceKind;
import org.springframework.stereotype.Component;

/** {@code RESTAURANTES}: parada com 2 noites ou mais e nenhum restaurante favorito. */
@Component
public class RestaurantsRule extends MissingFavoritesRule {

  public static final String CODE = "RESTAURANTES";

  public RestaurantsRule() {
    super(SavedPlaceKind.RESTAURANT, 2);
  }

  @Override
  public String code() {
    return CODE;
  }
}
