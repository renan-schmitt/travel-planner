package com.travelplanner.places.domain;

import java.util.Optional;

/**
 * Faixa de preço de um resultado do Places (ESP §3.1). Com {@code priceLevel}, ele manda; sem ele,
 * uma heurística pelo {@code primaryType} (exibida como "estimado"). O {@code priceRange} do Places
 * não é usado.
 */
public final class PriceTierMapper {

  private PriceTierMapper() {}

  /**
   * @param priceLevel enum do Places ({@code PRICE_LEVEL_MODERATE}; aceita também sem o prefixo)
   * @param primaryType tipo principal do lugar ({@code hotel}, {@code resort_hotel}, …)
   * @return vazio se não houver como estimar
   */
  public static Optional<PriceTier> map(String priceLevel, String primaryType) {
    return fromPriceLevel(priceLevel)
        .map(tier -> new PriceTier(tier, TierSource.PLACES))
        .or(() -> fromType(primaryType).map(tier -> new PriceTier(tier, TierSource.HEURISTIC)));
  }

  static Optional<Integer> fromPriceLevel(String priceLevel) {
    if (priceLevel == null) {
      return Optional.empty();
    }
    return switch (priceLevel.replaceFirst("^PRICE_LEVEL_", "")) {
      case "FREE", "INEXPENSIVE" -> Optional.of(1);
      case "MODERATE" -> Optional.of(2);
      case "EXPENSIVE" -> Optional.of(3);
      case "VERY_EXPENSIVE" -> Optional.of(4);
      default -> Optional.empty();
    };
  }

  static Optional<Integer> fromType(String primaryType) {
    if (primaryType == null) {
      return Optional.empty();
    }
    return switch (primaryType) {
      case "hostel", "motel", "campground" -> Optional.of(1);
      case "bed_and_breakfast", "guest_house" -> Optional.of(2);
      case "resort_hotel" -> Optional.of(3);
      default -> Optional.empty();
    };
  }
}
