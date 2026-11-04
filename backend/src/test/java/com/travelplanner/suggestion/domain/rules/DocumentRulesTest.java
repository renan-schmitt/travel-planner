package com.travelplanner.suggestion.domain.rules;

import static org.assertj.core.api.Assertions.assertThat;

import com.travelplanner.checklist.domain.ChecklistCategory;
import com.travelplanner.requirements.domain.PublishedRequirement;
import com.travelplanner.requirements.domain.RequirementType;
import com.travelplanner.suggestion.domain.Severity;
import com.travelplanner.suggestion.domain.SuggestionDraft;
import com.travelplanner.suggestion.domain.TripSnapshotFixtures;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class DocumentRulesTest {

  private final DestinationDocumentRule documents = new DestinationDocumentRule();
  private final DestinationWithoutRequirementsRule withoutData =
      new DestinationWithoutRequirementsRule();
  private final OutdatedRequirementRule outdated = new OutdatedRequirementRule();
  private final TripSnapshotFixtures trip = TripSnapshotFixtures.trip().now("2027-01-01T12:00:00Z");

  @Test
  void dueDateComesFromLeadDaysOrThirtyDaysForAVisa() {
    PublishedRequirement form =
        new PublishedRequirement(
            1, "DO", "BR", RequirementType.ENTRY_FORM, "e-Ticket", null, 3, "https://x", null);
    PublishedRequirement visa =
        new PublishedRequirement(
            2, "US", "BR", RequirementType.VISA, "Visto", null, null, "https://x", null);
    PublishedRequirement visaWithLead =
        new PublishedRequirement(
            3, "US", "BR", RequirementType.VISA, "Visto", null, 90, "https://x", null);
    PublishedRequirement other =
        new PublishedRequirement(
            4, "PT", "BR", RequirementType.OTHER, "Isenção", null, null, "https://x", null);
    LocalDate arrival = LocalDate.parse("2027-03-10");

    assertThat(form.dueDate(arrival)).isEqualTo("2027-03-07");
    assertThat(visa.dueDate(arrival)).isEqualTo("2027-02-08");
    assertThat(visaWithLead.dueDate(arrival)).isEqualTo("2026-12-10");
    assertThat(other.dueDate(arrival)).isNull();
    assertThat(form.dueDate(null)).isNull();
  }

  @Test
  void publishedRequirementCreatesTheChecklistItemWithDeadlineAndOfficialLink() {
    trip.traveler("Ana", "2030-01-01");
    trip.traveler("Bruno", "2030-01-01");
    trip.stop("Punta Cana", "DO", "America/Santo_Domingo", "2027-03-10", "2027-03-17");
    PublishedRequirement eticket =
        trip.requirement("DO", "BR", RequirementType.ENTRY_FORM, "Preencher o e-Ticket", 3, null);

    assertThat(documents.evaluate(trip.context()))
        .singleElement()
        .satisfies(
            d -> {
              assertThat(d.logicalKey()).isEqualTo(String.valueOf(eticket.id()));
              assertThat(d.severity()).isEqualTo(Severity.WARNING);
              assertThat(d.params())
                  .containsEntry("country", "República Dominicana")
                  .containsEntry("dueDate", "2027-03-07")
                  .containsEntry("travelers", "Ana, Bruno")
                  .containsEntry("officialUrl", "https://oficial.example/DO");
              assertThat(d.autoChecklist())
                  .hasValueSatisfying(
                      c -> {
                        assertThat(c.automatic()).isTrue();
                        assertThat(c.category()).isEqualTo(ChecklistCategory.DOCUMENTS);
                        assertThat(c.title())
                            .isEqualTo("Preencher o e-Ticket (República Dominicana)");
                        assertThat(c.dueDate()).isEqualTo("2027-03-07");
                        assertThat(c.notes()).endsWith("Fonte oficial: https://oficial.example/DO");
                      });
            });
    assertThat(withoutData.evaluate(trip.context())).isEmpty();
  }

  @Test
  void visaIsCriticalAndOnlyTheTravelersOfThatNationalityCount() {
    trip.traveler("Ana", "2030-01-01", "BR");
    trip.traveler("John", "2030-01-01", "US");
    trip.traveler("Lucía", "2030-01-01", "AR", "BR");
    trip.stop("Nova York", "US", "America/New_York", "2027-03-10", "2027-03-17");
    trip.requirement("US", "BR", RequirementType.VISA, "Visto americano", null, null);

    SuggestionDraft visa = documents.evaluate(trip.context()).getFirst();
    assertThat(visa.severity()).isEqualTo(Severity.CRITICAL);
    // John é cidadão americano; Lucía conta pela nacionalidade principal (AR).
    assertThat(visa.params()).containsEntry("travelers", "Ana");
    assertThat(visa.autoChecklist().orElseThrow().dueDate()).isEqualTo("2027-02-08");
    assertThat(withoutData.evaluate(trip.context()))
        .singleElement()
        .satisfies(
            d -> {
              assertThat(d.logicalKey()).isEqualTo("US");
              assertThat(d.severity()).isEqualTo(Severity.WARNING);
              assertThat(d.params()).containsEntry("nationalities", "AR");
            });
  }

  @Test
  void countryWithoutPublishedRequirementsIsFlaggedAndHomeIsIgnored() {
    trip.stop("Salvador", "BR", "America/Bahia", "2027-03-01", "2027-03-05");
    trip.stop("Lisboa", "2027-03-05", "2027-03-10");

    assertThat(documents.evaluate(trip.context())).isEmpty();
    assertThat(withoutData.evaluate(trip.context()))
        .singleElement()
        .satisfies(
            d -> {
              assertThat(d.logicalKey()).isEqualTo("PT");
              // Sem viajantes: conta a nacionalidade do dono.
              assertThat(d.params())
                  .containsEntry("country", "Portugal")
                  .containsEntry("nationalities", "BR");
            });
  }

  @Test
  void arrivalIsTheFirstStopInTheCountry() {
    trip.stop("Porto", "2027-03-15", "2027-03-18");
    trip.stop("Lisboa", "2027-03-10", "2027-03-15");
    trip.requirement("PT", "BR", RequirementType.ETA, "ETIAS", 4, null);

    assertThat(documents.evaluate(trip.context()).getFirst().params())
        .containsEntry("arrivalDate", "2027-03-10")
        .containsEntry("dueDate", "2027-03-06");
  }

  @Test
  void requirementsVerifiedMoreThanSixMonthsAgoAreOutdated() {
    trip.stop("Punta Cana", "DO", "America/Santo_Domingo", "2027-03-10", "2027-03-17");
    PublishedRequirement old =
        trip.requirement("DO", "BR", RequirementType.ENTRY_FORM, "e-Ticket", 3, "2026-06-30");
    trip.requirement("DO", "BR", RequirementType.OTHER, "Recente", null, "2026-07-01");
    PublishedRequirement otherNationality =
        trip.requirement("DO", "AR", RequirementType.OTHER, "Outra", null, "2020-01-01");

    assertThat(outdated.evaluate(trip.context()))
        .singleElement()
        .satisfies(
            d -> {
              assertThat(d.logicalKey()).isEqualTo(String.valueOf(old.id()));
              assertThat(d.severity()).isEqualTo(Severity.INFO);
              assertThat(d.params()).containsEntry("verifiedAt", "2026-06-30");
            });
    assertThat(otherNationality.id()).isNotEqualTo(old.id());
  }
}
