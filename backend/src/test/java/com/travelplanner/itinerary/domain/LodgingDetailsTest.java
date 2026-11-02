package com.travelplanner.itinerary.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.travelplanner.shared.web.InvalidRequestException;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class LodgingDetailsTest {

  /** Parada em Lisboa de 11/03 a 14/03. */
  private static final LocalDate ARRIVAL = LocalDate.parse("2027-03-11");

  private static final LocalDate DEPARTURE = LocalDate.parse("2027-03-14");

  private static LodgingDetails stay(String checkIn, String checkOut) {
    return new LodgingDetails(
        LodgingKind.HOTEL,
        "Hotel Avenida",
        null,
        LocalDate.parse(checkIn),
        LocalDate.parse(checkOut),
        BoardType.BREAKFAST,
        2,
        null,
        null);
  }

  @Test
  void acceptsStayOnTheStopLimits() {
    assertThatCode(() -> stay("2027-03-11", "2027-03-14").validate(ARRIVAL, DEPARTURE))
        .doesNotThrowAnyException();
    assertThat(stay("2027-03-11", "2027-03-14").nights()).isEqualTo(3);
  }

  @Test
  void acceptsTwoHotelsSplittingTheStop() {
    assertThatCode(() -> stay("2027-03-11", "2027-03-12").validate(ARRIVAL, DEPARTURE))
        .doesNotThrowAnyException();
    assertThatCode(() -> stay("2027-03-12", "2027-03-14").validate(ARRIVAL, DEPARTURE))
        .doesNotThrowAnyException();
  }

  @Test
  void friendsHomeHasNoBoardPriceOrBooking() {
    LodgingDetails home =
        new LodgingDetails(
            LodgingKind.FRIENDS_HOME,
            "Casa da tia Ana",
            null,
            ARRIVAL,
            DEPARTURE,
            BoardType.FULL_BOARD,
            3,
            "X1",
            "Levar presente");
    assertThat(home.boardType()).isNull();
    assertThat(home.priceTier()).isNull();
    assertThat(home.bookingRef()).isNull();
    assertThat(home.notes()).isEqualTo("Levar presente");
  }

  @Test
  void rejectsCheckOutNotAfterCheckIn() {
    assertThatThrownBy(() -> stay("2027-03-12", "2027-03-12").validate(ARRIVAL, DEPARTURE))
        .isInstanceOf(InvalidRequestException.class)
        .extracting("field")
        .isEqualTo("checkOut");
  }

  @Test
  void rejectsStayOutsideTheStop() {
    assertThatThrownBy(() -> stay("2027-03-10", "2027-03-12").validate(ARRIVAL, DEPARTURE))
        .extracting("field")
        .isEqualTo("checkIn");
    assertThatThrownBy(() -> stay("2027-03-12", "2027-03-15").validate(ARRIVAL, DEPARTURE))
        .hasMessageContaining("11/03 a 14/03")
        .extracting("field")
        .isEqualTo("checkOut");
  }
}
