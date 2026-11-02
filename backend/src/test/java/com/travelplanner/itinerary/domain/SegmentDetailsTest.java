package com.travelplanner.itinerary.domain;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.travelplanner.shared.web.InvalidRequestException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class SegmentDetailsTest {

  private static SegmentDetails travel(
      Long origin, Long destination, String depart, String departTz, String arrive, String tz) {
    return new SegmentDetails(
        TransportType.FLIGHT,
        origin,
        destination,
        null,
        null,
        null,
        LocalDateTime.parse(depart),
        departTz,
        arrive == null ? null : LocalDateTime.parse(arrive),
        tz,
        null,
        null,
        null,
        null);
  }

  private static SegmentDetails local(Long stop, Long origin) {
    return new SegmentDetails(
        TransportType.TRANSFER,
        origin,
        null,
        stop,
        "Aeroporto",
        "Hotel",
        LocalDateTime.parse("2027-03-11T12:00"),
        "Europe/Lisbon",
        null,
        null,
        null,
        null,
        null,
        null);
  }

  private static void assertInvalid(SegmentDetails details, String field) {
    assertThatThrownBy(details::validate)
        .isInstanceOfSatisfying(
            InvalidRequestException.class,
            e -> org.assertj.core.api.Assertions.assertThat(e.getField()).isEqualTo(field));
  }

  @Test
  void overnightFlightAcrossTimezonesIsValid() {
    // GRU 22:40 (GMT-3) → LIS 11:15 do dia seguinte (GMT+0): 9h35 de voo.
    assertThatCode(
            () ->
                travel(
                        null,
                        1L,
                        "2027-03-10T22:40",
                        "America/Sao_Paulo",
                        "2027-03-11T11:15",
                        "Europe/Lisbon")
                    .validate())
        .doesNotThrowAnyException();
  }

  @Test
  void arrivalEarlierOnTheLocalClockButLaterInRealTimeIsValid() {
    // LIS 12:00 (GMT+0) → GRU 19:00 (GMT-3): chega "às 19h" mas são 22h em Lisboa.
    // E um voo curto para oeste: LIS 10:00 → Açores 10:30 (GMT-1) chega "antes" de 11h em LIS.
    assertThatCode(
            () ->
                travel(
                        1L,
                        2L,
                        "2027-03-17T10:00",
                        "Europe/Lisbon",
                        "2027-03-17T09:50",
                        "Atlantic/Azores")
                    .validate())
        .doesNotThrowAnyException();
  }

  @Test
  void arrivalBeforeDepartureInRealTimeIsRejected() {
    // LIS 10:00 (GMT+0) → Roma 10:30 (GMT+1) = 09:30 em Lisboa: chegaria antes de partir.
    assertInvalid(
        travel(1L, 2L, "2027-03-17T10:00", "Europe/Lisbon", "2027-03-17T10:30", "Europe/Rome"),
        "arriveAt");
    assertInvalid(
        travel(1L, 2L, "2027-03-17T10:00", "Europe/Lisbon", "2027-03-17T10:00", "Europe/Lisbon"),
        "arriveAt");
  }

  @Test
  void arrivalIsOptionalButNeedsItsTimezone() {
    assertThatCode(() -> travel(1L, 2L, "2027-03-17T10:00", "Europe/Lisbon", null, null).validate())
        .doesNotThrowAnyException();
    assertInvalid(
        travel(1L, 2L, "2027-03-17T10:00", "Europe/Lisbon", "2027-03-17T12:00", null), "arriveTz");
  }

  @Test
  void travelNeedsDifferentEnds() {
    assertInvalid(
        travel(1L, 1L, "2027-03-17T10:00", "Europe/Lisbon", null, null), "destinationStopId");
    // casa → casa
    assertInvalid(
        travel(null, null, "2027-03-17T10:00", "Europe/Lisbon", null, null), "destinationStopId");
  }

  @Test
  void localSegmentHasNoOriginNorDestination() {
    assertThatCode(() -> local(1L, null).validate()).doesNotThrowAnyException();
    assertInvalid(local(1L, 2L), "localStopId");
  }
}
