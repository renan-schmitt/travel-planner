package com.travelplanner.suggestion.domain.rules;

import static com.travelplanner.suggestion.domain.TripSnapshotFixtures.LIS;
import static com.travelplanner.suggestion.domain.TripSnapshotFixtures.SP;
import static org.assertj.core.api.Assertions.assertThat;

import com.travelplanner.itinerary.domain.TransportType;
import com.travelplanner.suggestion.domain.TripSnapshotFixtures;
import org.junit.jupiter.api.Test;

class TransferDepartureRuleTest {

  private final TransferDepartureRule rule = new TransferDepartureRule();
  private final TripSnapshotFixtures trip = TripSnapshotFixtures.trip();

  @Test
  void flightFromStopWithoutTransferIsAGap() {
    var lisbon = trip.stop("Lisboa", "2027-03-11", "2027-03-15");
    var flight = trip.flight(lisbon, null, "2027-03-15T10:00", LIS, "2027-03-15T18:00", SP);

    assertThat(rule.evaluate(trip.context()))
        .singleElement()
        .satisfies(
            d -> {
              assertThat(d.logicalKey()).isEqualTo(String.valueOf(flight.id()));
              assertThat(d.params())
                  .containsEntry("fromHome", false)
                  .containsEntry("city", "Lisboa");
            });
  }

  @Test
  void localTransferArrivingWithinEightHoursCovers() {
    var lisbon = trip.stop("Lisboa", "2027-03-11", "2027-03-15");
    trip.flight(lisbon, null, "2027-03-15T10:00", LIS, "2027-03-15T18:00", SP);
    trip.local(TransportType.TAXI_RIDE, lisbon, "2027-03-15T07:00", "2027-03-15T07:40");

    assertThat(rule.evaluate(trip.context())).isEmpty();
  }

  @Test
  void groundSegmentEndingAtTheOriginCovers() {
    var porto = trip.stop("Porto", "2027-03-11", "2027-03-14");
    var lisbon = trip.stop("Lisboa", "2027-03-14", "2027-03-15");
    trip.segment(
        TransportType.TRAIN, porto, lisbon, "2027-03-15T04:00", LIS, "2027-03-15T07:00", LIS);
    trip.flight(lisbon, null, "2027-03-15T10:00", LIS, "2027-03-15T18:00", SP);

    assertThat(rule.evaluate(trip.context())).isEmpty();
  }

  @Test
  void transferTooEarlyOrAfterTheDepartureDoesNotCount() {
    var lisbon = trip.stop("Lisboa", "2027-03-11", "2027-03-15");
    trip.flight(lisbon, null, "2027-03-15T10:00", LIS, "2027-03-15T18:00", SP);
    trip.local(TransportType.TRANSFER, lisbon, "2027-03-15T01:00", "2027-03-15T01:59");
    trip.local(TransportType.TRANSFER, lisbon, "2027-03-15T10:30", null);

    assertThat(rule.evaluate(trip.context())).hasSize(1);
  }

  @Test
  void flightFromHomeIsAlwaysAGapUntilDismissed() {
    var lisbon = trip.stop("Lisboa", "2027-03-11", "2027-03-15");
    trip.flight(null, lisbon, "2027-03-10T22:40", SP, "2027-03-11T11:15", LIS);

    assertThat(rule.evaluate(trip.context()))
        .singleElement()
        .satisfies(d -> assertThat(d.params()).containsEntry("fromHome", true));
  }
}
