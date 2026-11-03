package com.travelplanner.suggestion.domain.rules;

import static org.assertj.core.api.Assertions.assertThat;

import com.travelplanner.suggestion.domain.Severity;
import com.travelplanner.suggestion.domain.TripSnapshotFixtures;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class TravelInsuranceRuleTest {

  private final TravelInsuranceRule rule = new TravelInsuranceRule();
  private final TripSnapshotFixtures trip = TripSnapshotFixtures.trip();

  @Test
  void schengenTripWithoutInsuranceIsAWarningWithChecklistOnAccept() {
    trip.stop("Lisboa", "2027-03-11", "2027-03-20");

    assertThat(rule.evaluate(trip.context()))
        .singleElement()
        .satisfies(
            d -> {
              assertThat(d.logicalKey()).isEqualTo("trip");
              assertThat(d.severity()).isEqualTo(Severity.WARNING);
              assertThat(d.autoChecklist())
                  .hasValueSatisfying(
                      c -> {
                        assertThat(c.automatic()).isFalse();
                        assertThat(c.dueDate()).isEqualTo(LocalDate.parse("2027-02-25"));
                      });
            });
  }

  @Test
  void outsideSchengenItIsInfo() {
    trip.stop("Nova York", "US", "America/New_York", "2027-03-11", "2027-03-20");

    assertThat(rule.evaluate(trip.context()))
        .singleElement()
        .satisfies(d -> assertThat(d.severity()).isEqualTo(Severity.INFO));
  }

  @Test
  void anInsuranceItemInTheChecklistResolves() {
    trip.stop("Lisboa", "2027-03-11", "2027-03-20");
    trip.checklistItem(null, "Contratar  Seguro-viagem".replace("-", " "));
    assertThat(rule.evaluate(trip.context())).isEmpty();

    var other = TripSnapshotFixtures.trip();
    other.stop("Lisboa", "2027-03-11", "2027-03-20");
    other.checklistItem(TravelInsuranceRule.CODE, "Qualquer título");
    assertThat(rule.evaluate(other.context())).isEmpty();
  }

  @Test
  void domesticTripIsIgnored() {
    trip.stop("Salvador", "BR", "America/Bahia", "2027-03-11", "2027-03-20");

    assertThat(rule.evaluate(trip.context())).isEmpty();
  }
}
