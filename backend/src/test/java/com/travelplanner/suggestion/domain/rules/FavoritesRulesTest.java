package com.travelplanner.suggestion.domain.rules;

import static org.assertj.core.api.Assertions.assertThat;

import com.travelplanner.itinerary.domain.SavedPlaceKind;
import com.travelplanner.itinerary.domain.TripSnapshot.StopEntry;
import com.travelplanner.suggestion.domain.Severity;
import com.travelplanner.suggestion.domain.SuggestionDraft;
import com.travelplanner.suggestion.domain.TripSnapshotFixtures;
import org.junit.jupiter.api.Test;

/** {@code RESTAURANTES} e {@code ATRACOES}. */
class FavoritesRulesTest {

  private final RestaurantsRule restaurants = new RestaurantsRule();
  private final AttractionsRule attractions = new AttractionsRule();
  private final TripSnapshotFixtures trip = TripSnapshotFixtures.trip();

  @Test
  void stopWithoutFavoritesGetsBothSuggestions() {
    StopEntry lisboa = trip.stop("Lisboa", "2027-03-11", "2027-03-14");

    assertThat(restaurants.evaluate(trip.context()))
        .singleElement()
        .satisfies(
            d -> {
              assertThat(d.logicalKey()).isEqualTo(String.valueOf(lisboa.id()));
              assertThat(d.severity()).isEqualTo(Severity.INFO);
              assertThat(d.params())
                  .containsEntry("stopId", lisboa.id())
                  .containsEntry("city", "Lisboa")
                  .containsEntry("nights", 3);
              assertThat(d.autoChecklist()).isEmpty();
            });
    assertThat(attractions.evaluate(trip.context()))
        .extracting(SuggestionDraft::logicalKey)
        .containsExactly(String.valueOf(lisboa.id()));
  }

  @Test
  void minimumNightsDifferPerRule() {
    trip.stop("Sintra", "2027-03-11", "2027-03-11"); // bate-volta: nada
    StopEntry porto = trip.stop("Porto", "2027-03-11", "2027-03-12"); // 1 noite: só atrações

    assertThat(restaurants.evaluate(trip.context())).isEmpty();
    assertThat(attractions.evaluate(trip.context()))
        .extracting(SuggestionDraft::logicalKey)
        .containsExactly(String.valueOf(porto.id()));
  }

  @Test
  void favoriteOfTheRightKindInTheSameStopResolves() {
    StopEntry lisboa = trip.stop("Lisboa", "2027-03-11", "2027-03-14");
    StopEntry porto = trip.stop("Porto", "2027-03-14", "2027-03-17");
    trip.savedPlace(lisboa, SavedPlaceKind.RESTAURANT);
    trip.savedPlace(porto, SavedPlaceKind.ATTRACTION);
    trip.savedPlace(porto, SavedPlaceKind.OTHER);

    assertThat(restaurants.evaluate(trip.context()))
        .extracting(SuggestionDraft::logicalKey)
        .containsExactly(String.valueOf(porto.id()));
    assertThat(attractions.evaluate(trip.context()))
        .extracting(SuggestionDraft::logicalKey)
        .containsExactly(String.valueOf(lisboa.id()));
  }
}
