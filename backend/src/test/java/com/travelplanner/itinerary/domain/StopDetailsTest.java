package com.travelplanner.itinerary.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class StopDetailsTest {

  private static StopDetails stop(String arrival, String departure) {
    return new StopDetails(
        null,
        "Lisboa",
        "PT",
        null,
        null,
        "Europe/Lisbon",
        LocalDate.parse(arrival),
        LocalDate.parse(departure),
        null);
  }

  private static boolean overlap(String a1, String d1, String a2, String d2) {
    return stop(a1, d1).overlapsMoreThanOneDay(LocalDate.parse(a2), LocalDate.parse(d2));
  }

  @Test
  void sharingTheChangeoverDayIsAllowed() {
    assertThat(overlap("2027-03-10", "2027-03-14", "2027-03-14", "2027-03-17")).isFalse();
    assertThat(overlap("2027-03-14", "2027-03-17", "2027-03-10", "2027-03-14")).isFalse();
  }

  @Test
  void disjointStopsDoNotOverlap() {
    assertThat(overlap("2027-03-10", "2027-03-12", "2027-03-15", "2027-03-17")).isFalse();
  }

  @Test
  void dayTripInsideAnotherStopIsAllowed() {
    assertThat(overlap("2027-03-12", "2027-03-12", "2027-03-10", "2027-03-14")).isFalse();
  }

  @Test
  void moreThanOneSharedDayOverlaps() {
    assertThat(overlap("2027-03-10", "2027-03-14", "2027-03-13", "2027-03-17")).isTrue();
    assertThat(overlap("2027-03-10", "2027-03-20", "2027-03-12", "2027-03-14")).isTrue();
    assertThat(overlap("2027-03-10", "2027-03-14", "2027-03-10", "2027-03-14")).isTrue();
  }

  @Test
  void departureBeforeArrivalIsRejected() {
    assertThatThrownBy(() -> stop("2027-03-14", "2027-03-10"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
