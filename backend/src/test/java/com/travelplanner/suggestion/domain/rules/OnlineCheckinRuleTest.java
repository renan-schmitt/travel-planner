package com.travelplanner.suggestion.domain.rules;

import static com.travelplanner.suggestion.domain.TripSnapshotFixtures.LIS;
import static com.travelplanner.suggestion.domain.TripSnapshotFixtures.SP;
import static org.assertj.core.api.Assertions.assertThat;

import com.travelplanner.checklist.domain.ChecklistCategory;
import com.travelplanner.itinerary.domain.TransportType;
import com.travelplanner.suggestion.domain.SuggestionDraft;
import com.travelplanner.suggestion.domain.TripSnapshotFixtures;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class OnlineCheckinRuleTest {

  private final OnlineCheckinRule rule = new OnlineCheckinRule();
  private final TripSnapshotFixtures trip = TripSnapshotFixtures.trip();

  @Test
  void flightWithinThreeDaysCreatesTheChecklistItem() {
    // 22:40 em São Paulo = 01:40Z do dia 11.
    trip.now("2027-03-08T01:40:00Z");
    var lisbon = trip.stop("Lisboa", "2027-03-11", "2027-03-15");
    var flight = trip.flight(null, lisbon, "2027-03-10T22:40", SP, "2027-03-11T11:15", LIS);

    assertThat(rule.evaluate(trip.context()))
        .singleElement()
        .satisfies(
            d -> {
              assertThat(d.logicalKey()).isEqualTo(String.valueOf(flight.id()));
              assertThat(d.autoChecklist())
                  .hasValueSatisfying(
                      c -> {
                        assertThat(c.automatic()).isTrue();
                        assertThat(c.category()).isEqualTo(ChecklistCategory.BOOKINGS);
                        assertThat(c.title())
                            .isEqualTo("Check-in online TAP " + flight.details().code());
                        assertThat(c.dueDate()).isEqualTo(LocalDate.parse("2027-03-09"));
                      });
            });
  }

  @Test
  void flightsTooFarAwayOrAlreadyGoneAreIgnored() {
    trip.now("2027-03-08T01:39:00Z");
    var lisbon = trip.stop("Lisboa", "2027-03-11", "2027-03-15");
    trip.flight(null, lisbon, "2027-03-10T22:40", SP, "2027-03-11T11:15", LIS);
    trip.flight(lisbon, null, "2027-03-07T10:00", LIS, "2027-03-07T18:00", SP);
    trip.segment(TransportType.TRAIN, lisbon, null, "2027-03-09T10:00", LIS, null, null);

    assertThat(rule.evaluate(trip.context())).extracting(SuggestionDraft::logicalKey).isEmpty();
  }
}
