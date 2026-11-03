package com.travelplanner.suggestion.domain.rules;

import static org.assertj.core.api.Assertions.assertThat;

import com.travelplanner.itinerary.domain.TransportType;
import com.travelplanner.suggestion.domain.Severity;
import com.travelplanner.suggestion.domain.SuggestionDraft;
import com.travelplanner.suggestion.domain.TripSnapshotFixtures;
import org.junit.jupiter.api.Test;

class LocalTransportRuleTest {

  private final LocalTransportRule rule = new LocalTransportRule();
  private final TripSnapshotFixtures trip = TripSnapshotFixtures.trip();

  @Test
  void stopsWithTwoNightsOrMoreWithoutCarRental() {
    var lisbon = trip.stop("Lisboa", "2027-03-11", "2027-03-13");
    trip.stop("Sintra", "2027-03-13", "2027-03-14");

    assertThat(rule.evaluate(trip.context()))
        .singleElement()
        .satisfies(
            d -> {
              assertThat(d.logicalKey()).isEqualTo(String.valueOf(lisbon.id()));
              assertThat(d.severity()).isEqualTo(Severity.INFO);
              assertThat(d.params()).containsEntry("nights", 2L);
            });
  }

  @Test
  void carRentalAtTheStopResolves() {
    var lisbon = trip.stop("Lisboa", "2027-03-11", "2027-03-14");
    var porto = trip.stop("Porto", "2027-03-14", "2027-03-17");
    trip.local(TransportType.CAR_RENTAL, lisbon, "2027-03-11T12:00", "2027-03-14T10:00");
    trip.local(TransportType.TRANSFER, porto, "2027-03-14T12:00", null);

    assertThat(rule.evaluate(trip.context()))
        .extracting(SuggestionDraft::logicalKey)
        .containsExactly(String.valueOf(porto.id()));
  }
}
