package com.travelplanner.suggestion.domain.rules;

import static com.travelplanner.suggestion.domain.TripSnapshotFixtures.LIS;
import static com.travelplanner.suggestion.domain.TripSnapshotFixtures.SP;
import static org.assertj.core.api.Assertions.assertThat;

import com.travelplanner.itinerary.domain.TransportType;
import com.travelplanner.suggestion.domain.Severity;
import com.travelplanner.suggestion.domain.SuggestionDraft;
import com.travelplanner.suggestion.domain.TripSnapshotFixtures;
import org.junit.jupiter.api.Test;

class TransferArrivalRuleTest {

  private final TransferArrivalRule rule = new TransferArrivalRule();
  private final TripSnapshotFixtures trip = TripSnapshotFixtures.trip();

  @Test
  void flightToStopWithoutTransferIsAGap() {
    var lisbon = trip.stop("Lisboa", "2027-03-11", "2027-03-15");
    var flight = trip.flight(null, lisbon, "2027-03-10T22:40", SP, "2027-03-11T11:15", LIS);

    var drafts = rule.evaluate(trip.context());

    assertThat(drafts)
        .singleElement()
        .satisfies(
            d -> {
              assertThat(d.logicalKey()).isEqualTo(String.valueOf(flight.id()));
              assertThat(d.severity()).isEqualTo(Severity.WARNING);
              assertThat(d.params())
                  .containsEntry("city", "Lisboa")
                  .containsEntry("stopId", lisbon.id());
            });
  }

  @Test
  void localTransferWithinSixHoursCoversTheArrival() {
    var lisbon = trip.stop("Lisboa", "2027-03-11", "2027-03-15");
    trip.flight(null, lisbon, "2027-03-10T22:40", SP, "2027-03-11T11:15", LIS);
    trip.local(TransportType.TRANSFER, lisbon, "2027-03-11T12:00", null);

    assertThat(rule.evaluate(trip.context())).isEmpty();
  }

  @Test
  void groundSegmentLeavingTheDestinationCoversTheArrival() {
    var lisbon = trip.stop("Lisboa", "2027-03-11", "2027-03-11");
    var porto = trip.stop("Porto", "2027-03-11", "2027-03-15");
    trip.flight(null, lisbon, "2027-03-10T22:40", SP, "2027-03-11T11:15", LIS);
    trip.segment(
        TransportType.TRAIN, lisbon, porto, "2027-03-11T14:00", LIS, "2027-03-11T17:00", LIS);

    assertThat(rule.evaluate(trip.context())).isEmpty();
  }

  @Test
  void transferAfterSixHoursOrBeforeTheLandingDoesNotCount() {
    var lisbon = trip.stop("Lisboa", "2027-03-11", "2027-03-15");
    trip.flight(null, lisbon, "2027-03-10T22:40", SP, "2027-03-11T11:15", LIS);
    trip.local(TransportType.TRANSFER, lisbon, "2027-03-11T17:16", null);
    trip.local(TransportType.TRANSFER, lisbon, "2027-03-11T11:00", null);

    assertThat(rule.evaluate(trip.context())).hasSize(1);
  }

  @Test
  void comparesInRealTimeAcrossTimeZones() {
    // Chega 11:15 em Lisboa (= 08:15 em São Paulo); um transfer marcado "12:00" no fuso de São
    // Paulo é 15:00 em Lisboa: dentro das 6 h.
    var lisbon = trip.stop("Lisboa", "2027-03-11", "2027-03-15");
    trip.flight(null, lisbon, "2027-03-10T22:40", SP, "2027-03-11T11:15", LIS);
    trip.segment(TransportType.TRANSFER, lisbon, null, "2027-03-11T12:00", SP, null, null);
    // Saindo de Lisboa às 12:00 de SP (15:00 de Lisboa): conta como "sai do destino".

    assertThat(rule.evaluate(trip.context())).isEmpty();
  }

  @Test
  void flightArrivingHomeIsIgnored() {
    var lisbon = trip.stop("Lisboa", "2027-03-11", "2027-03-15");
    trip.flight(lisbon, null, "2027-03-15T10:00", LIS, "2027-03-15T18:00", SP);

    assertThat(rule.evaluate(trip.context())).isEmpty();
  }

  @Test
  void flightWithoutArrivalTimeUsesTheDeparture() {
    var lisbon = trip.stop("Lisboa", "2027-03-11", "2027-03-15");
    trip.flight(null, lisbon, "2027-03-11T08:00", LIS, null, null);
    trip.local(TransportType.TRANSFER, lisbon, "2027-03-11T13:00", null);

    assertThat(rule.evaluate(trip.context())).extracting(SuggestionDraft::logicalKey).isEmpty();
  }
}
