package com.travelplanner.suggestion.domain.rules;

import static org.assertj.core.api.Assertions.assertThat;

import com.travelplanner.suggestion.domain.Severity;
import com.travelplanner.suggestion.domain.SuggestionDraft;
import com.travelplanner.suggestion.domain.TripSnapshotFixtures;
import org.junit.jupiter.api.Test;

class PassportValidityRuleTest {

  private final PassportValidityRule rule = new PassportValidityRule();
  private final TripSnapshotFixtures trip = TripSnapshotFixtures.trip();

  @Test
  void missingAndExpiringPassportsInAnInternationalTrip() {
    trip.stop("Lisboa", "2027-03-11", "2027-03-20");
    var ana = trip.traveler("Ana", null);
    var bia = trip.traveler("Bia", "2027-09-19");
    trip.traveler("Caio", "2027-09-20");

    var drafts = rule.evaluate(trip.context());

    assertThat(drafts)
        .extracting(SuggestionDraft::logicalKey, SuggestionDraft::severity)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple(String.valueOf(ana.id()), Severity.INFO),
            org.assertj.core.groups.Tuple.tuple(String.valueOf(bia.id()), Severity.CRITICAL));
    assertThat(drafts.get(0).autoChecklist()).isEmpty();
    assertThat(drafts.get(1).params()).containsEntry("limit", "2027-09-20");
    assertThat(drafts.get(1).autoChecklist())
        .hasValueSatisfying(c -> assertThat(c.title()).isEqualTo("Renovar passaporte de Bia"));
  }

  @Test
  void domesticTripIsIgnored() {
    trip.stop("Salvador", "BR", "America/Bahia", "2027-03-11", "2027-03-20");
    trip.traveler("Ana", null);

    assertThat(rule.evaluate(trip.context())).isEmpty();
  }

  @Test
  void braziliansGoingOnlyWhereTheIdCardIsAcceptedAreSkipped() {
    trip.stop("Buenos Aires", "AR", "America/Argentina/Buenos_Aires", "2027-03-11", "2027-03-15");
    trip.stop("Montevidéu", "UY", "America/Montevideo", "2027-03-15", "2027-03-20");
    trip.traveler("Ana", null, "BR");
    var john = trip.traveler("John", null, "US");

    assertThat(rule.evaluate(trip.context()))
        .extracting(SuggestionDraft::logicalKey)
        .containsExactly(String.valueOf(john.id()));
  }

  @Test
  void homeCountryComesFromTheHomeCityThenTheNationality() {
    trip.home("Lisboa", "PT", "Europe/Lisbon");
    trip.stop("Porto", "2027-03-11", "2027-03-20");
    trip.traveler("Ana", null);
    assertThat(rule.evaluate(trip.context())).isEmpty();

    var abroad = TripSnapshotFixtures.trip().noHome("PT");
    abroad.stop("Madri", "ES", "Europe/Madrid", "2027-03-11", "2027-03-20");
    abroad.traveler("Ana", null);
    assertThat(rule.evaluate(abroad.context())).hasSize(1);
  }
}
