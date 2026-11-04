package com.travelplanner.places.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PriceTierMapperTest {

  @ParameterizedTest
  @CsvSource({
    "PRICE_LEVEL_FREE, hotel, 1",
    "PRICE_LEVEL_INEXPENSIVE, resort_hotel, 1",
    "PRICE_LEVEL_MODERATE, hotel, 2",
    "PRICE_LEVEL_EXPENSIVE, hostel, 3",
    "PRICE_LEVEL_VERY_EXPENSIVE, hotel, 4",
    "MODERATE, hotel, 2",
  })
  void priceLevelWinsOverType(String priceLevel, String type, int tier) {
    assertThat(PriceTierMapper.map(priceLevel, type))
        .contains(new PriceTier(tier, TierSource.PLACES));
  }

  @ParameterizedTest
  @CsvSource({
    "hostel, 1",
    "motel, 1",
    "campground, 1",
    "bed_and_breakfast, 2",
    "guest_house, 2",
    "resort_hotel, 3",
  })
  void withoutPriceLevelEstimatesFromType(String type, int tier) {
    assertThat(PriceTierMapper.map(null, type)).contains(new PriceTier(tier, TierSource.HEURISTIC));
    assertThat(PriceTierMapper.map("PRICE_LEVEL_UNSPECIFIED", type))
        .contains(new PriceTier(tier, TierSource.HEURISTIC));
  }

  @ParameterizedTest
  @CsvSource(
      value = {"hotel", "lodging", "NULL"},
      nullValues = "NULL")
  void unknownWithoutPriceLevel(String type) {
    assertThat(PriceTierMapper.map(null, type)).isEmpty();
  }
}
