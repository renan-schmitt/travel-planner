package com.travelplanner.suggestion.domain.rules;

import static org.assertj.core.api.Assertions.assertThat;

import com.travelplanner.checklist.domain.ChecklistCategory;
import com.travelplanner.itinerary.domain.BoardType;
import com.travelplanner.itinerary.domain.LodgingKind;
import com.travelplanner.itinerary.domain.TripSnapshot.LodgingEntry;
import com.travelplanner.itinerary.domain.TripSnapshot.StopEntry;
import com.travelplanner.suggestion.domain.Severity;
import com.travelplanner.suggestion.domain.SuggestionDraft;
import com.travelplanner.suggestion.domain.TripSnapshotFixtures;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ResortOptionsRuleTest {

  private final ResortOptionsRule rule = new ResortOptionsRule();
  private final TripSnapshotFixtures trip = TripSnapshotFixtures.trip();

  @Test
  void allInclusiveLodgingSuggestsTheResortRestaurants() {
    StopEntry stop =
        trip.stop("Punta Cana", "DO", "America/Santo_Domingo", "2027-03-11", "2027-03-18");
    LodgingEntry hotel =
        trip.lodging(
            stop,
            LodgingKind.HOTEL,
            "Barceló Bávaro Palace",
            BoardType.ALL_INCLUSIVE,
            "ChIJBarcelo",
            "2027-03-11",
            "2027-03-18");

    assertThat(rule.evaluate(trip.context()))
        .singleElement()
        .satisfies(
            d -> {
              assertThat(d.logicalKey()).isEqualTo(String.valueOf(hotel.id()));
              assertThat(d.severity()).isEqualTo(Severity.INFO);
              assertThat(d.params())
                  .containsEntry("lodgingId", hotel.id())
                  .containsEntry("stopId", stop.id())
                  .containsEntry("city", "Punta Cana")
                  .containsEntry("placeId", "ChIJBarcelo")
                  .containsEntry("allInclusive", true);
              assertThat(d.autoChecklist())
                  .hasValueSatisfying(
                      c -> {
                        assertThat(c.automatic()).isFalse();
                        assertThat(c.category()).isEqualTo(ChecklistCategory.BOOKINGS);
                        assertThat(c.title()).contains("à la carte").contains("Barceló");
                        assertThat(c.dueDate()).isEqualTo(LocalDate.parse("2027-03-10"));
                      });
            });
  }

  @Test
  void resortInTheNameCountsWithoutPlaceIdAndAccents() {
    StopEntry stop =
        trip.stop("Porto de Galinhas", "BR", "America/Recife", "2027-03-11", "2027-03-14");
    trip.lodging(
        stop,
        LodgingKind.HOTEL,
        "Enotel RESÓRT & Spa",
        BoardType.BREAKFAST,
        null,
        "2027-03-11",
        "2027-03-14");

    assertThat(rule.evaluate(trip.context()))
        .singleElement()
        .satisfies(
            d -> {
              assertThat(d.params()).doesNotContainKey("placeId");
              assertThat(d.params()).containsEntry("allInclusive", false);
            });
  }

  @Test
  void ordinaryHotelsAndFriendsHomesDoNotCount() {
    StopEntry stop = trip.stop("Lisboa", "2027-03-11", "2027-03-14");
    trip.lodging(stop, "2027-03-11", "2027-03-12");
    trip.lodging(
        stop,
        LodgingKind.FRIENDS_HOME,
        "Casa do Resort da tia",
        null,
        null,
        "2027-03-12",
        "2027-03-14");

    assertThat(rule.evaluate(trip.context())).extracting(SuggestionDraft::logicalKey).isEmpty();
  }
}
