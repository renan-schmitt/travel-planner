package com.travelplanner.suggestion.domain.rules;

import static com.travelplanner.suggestion.domain.TripSnapshotFixtures.LIS;
import static com.travelplanner.suggestion.domain.TripSnapshotFixtures.SP;
import static org.assertj.core.api.Assertions.assertThat;

import com.travelplanner.itinerary.domain.TransportType;
import com.travelplanner.suggestion.domain.SuggestionDraft;
import com.travelplanner.suggestion.domain.TripSnapshotFixtures;
import org.junit.jupiter.api.Test;

class MissingTransportRuleTest {

  private final MissingTransportRule rule = new MissingTransportRule();
  private final TripSnapshotFixtures trip = TripSnapshotFixtures.trip();

  @Test
  void everyLegWithoutSegmentIsAGap() {
    var lisbon = trip.stop("Lisboa", "2027-03-11", "2027-03-14");
    var porto = trip.stop("Porto", "2027-03-14", "2027-03-17");

    assertThat(rule.evaluate(trip.context()))
        .extracting(SuggestionDraft::logicalKey)
        .containsExactly(
            "home-" + lisbon.id(), lisbon.id() + "-" + porto.id(), porto.id() + "-home");
    assertThat(rule.evaluate(trip.context()).get(1).params())
        .containsEntry("fromCity", "Lisboa")
        .containsEntry("toCity", "Porto")
        .containsEntry("fromHome", false);
  }

  @Test
  void segmentsCloseTheLegs() {
    var lisbon = trip.stop("Lisboa", "2027-03-11", "2027-03-14");
    var porto = trip.stop("Porto", "2027-03-14", "2027-03-17");
    trip.flight(null, lisbon, "2027-03-10T22:40", SP, "2027-03-11T11:15", LIS);
    trip.segment(
        TransportType.TRAIN, lisbon, porto, "2027-03-14T09:00", LIS, "2027-03-14T12:00", LIS);
    // Trecho local não fecha a perna Porto → casa.
    trip.local(TransportType.CAR_RENTAL, porto, "2027-03-14T13:00", "2027-03-17T10:00");

    assertThat(rule.evaluate(trip.context()))
        .extracting(SuggestionDraft::logicalKey)
        .containsExactly(porto.id() + "-home");
  }

  @Test
  void segmentInTheWrongDirectionDoesNotCount() {
    var lisbon = trip.stop("Lisboa", "2027-03-11", "2027-03-14");
    trip.flight(lisbon, null, "2027-03-14T10:00", LIS, "2027-03-14T18:00", SP);

    assertThat(rule.evaluate(trip.context()))
        .extracting(SuggestionDraft::logicalKey)
        .containsExactly("home-" + lisbon.id());
  }

  @Test
  void tripWithoutStopsHasNoLegs() {
    assertThat(rule.evaluate(trip.context())).isEmpty();
  }
}
