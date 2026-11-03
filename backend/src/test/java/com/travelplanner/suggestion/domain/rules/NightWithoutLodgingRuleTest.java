package com.travelplanner.suggestion.domain.rules;

import static com.travelplanner.suggestion.domain.TripSnapshotFixtures.LIS;
import static com.travelplanner.suggestion.domain.TripSnapshotFixtures.SP;
import static org.assertj.core.api.Assertions.assertThat;

import com.travelplanner.itinerary.domain.TransportType;
import com.travelplanner.suggestion.domain.SuggestionDraft;
import com.travelplanner.suggestion.domain.TripSnapshotFixtures;
import org.junit.jupiter.api.Test;

class NightWithoutLodgingRuleTest {

  private final NightWithoutLodgingRule rule = new NightWithoutLodgingRule();
  private final TripSnapshotFixtures trip = TripSnapshotFixtures.trip();

  @Test
  void everyUncoveredNightAtAStopIsAGap() {
    var lisbon = trip.stop("Lisboa", "2027-03-11", "2027-03-14");
    trip.lodging(lisbon, "2027-03-12", "2027-03-13");

    assertThat(rule.evaluate(trip.context()))
        .extracting(SuggestionDraft::logicalKey)
        .containsExactly("2027-03-11", "2027-03-13");
    assertThat(rule.evaluate(trip.context()).getFirst().params())
        .containsEntry("city", "Lisboa")
        .containsEntry("date", "2027-03-11");
  }

  @Test
  void nightsAtHomeAndOvernightFlightsAreNotGaps() {
    var lisbon = trip.stop("Lisboa", "2027-03-11", "2027-03-13");
    trip.dates("2027-03-10", "2027-03-14");
    trip.flight(null, lisbon, "2027-03-10T22:40", SP, "2027-03-11T11:15", LIS);
    trip.lodging(lisbon, "2027-03-11", "2027-03-13");
    trip.flight(lisbon, null, "2027-03-13T23:00", LIS, "2027-03-14T06:00", SP);

    assertThat(rule.evaluate(trip.context())).isEmpty();
  }

  @Test
  void nightBetweenStopsIsAGapAttributedToTheEarlierStop() {
    var lisbon = trip.stop("Lisboa", "2027-03-11", "2027-03-13");
    var porto = trip.stop("Porto", "2027-03-14", "2027-03-16");
    trip.lodging(lisbon, "2027-03-11", "2027-03-13");
    trip.lodging(porto, "2027-03-14", "2027-03-16");
    trip.segment(
        TransportType.BUS, lisbon, porto, "2027-03-14T09:00", LIS, "2027-03-14T12:00", LIS);

    assertThat(rule.evaluate(trip.context()))
        .extracting(SuggestionDraft::logicalKey)
        .containsExactly("2027-03-13");
  }
}
