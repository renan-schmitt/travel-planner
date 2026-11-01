package com.travelplanner.trip.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TripStatusTest {

  private static final LocalDate TODAY = LocalDate.of(2027, 3, 10);

  @ParameterizedTest(name = "{0} {1}..{2} → {3}")
  @CsvSource(
      nullValues = "-",
      value = {
        "PLANNING,  -,          -,          PLANNING",
        "CONFIRMED, -,          -,          CONFIRMED",
        "CONFIRMED, 2027-03-11, 2027-03-20, CONFIRMED",
        "PLANNING,  2027-03-10, 2027-03-20, IN_PROGRESS",
        "CONFIRMED, 2027-03-01, 2027-03-10, IN_PROGRESS",
        "CONFIRMED, 2027-03-01, 2027-03-09, DONE",
        "PLANNING,  2027-03-01, -,          IN_PROGRESS",
        "PLANNING,  2027-03-11, -,          PLANNING",
        "PLANNING,  -,          2027-03-09, DONE",
        "PLANNING,  -,          2027-03-10, PLANNING",
      })
  void effectiveStatus(TripStatus stored, LocalDate start, LocalDate end, TripStatus expected) {
    assertThat(TripStatus.effective(stored, start, end, TODAY)).isEqualTo(expected);
  }
}
