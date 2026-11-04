package com.travelplanner.itinerary.domain;

import static com.travelplanner.itinerary.domain.ItineraryEventKind.CHECK_IN;
import static com.travelplanner.itinerary.domain.ItineraryEventKind.CHECK_OUT;
import static com.travelplanner.itinerary.domain.ItineraryEventKind.SEGMENT;
import static com.travelplanner.itinerary.domain.ItineraryEventKind.SEGMENT_ARRIVAL;
import static com.travelplanner.itinerary.domain.ItineraryEventKind.SEGMENT_DEPARTURE;
import static org.assertj.core.api.Assertions.assertThat;

import com.travelplanner.itinerary.domain.TripSnapshot.LodgingEntry;
import com.travelplanner.itinerary.domain.TripSnapshot.SegmentEntry;
import com.travelplanner.itinerary.domain.TripSnapshot.StopEntry;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ItineraryBuilderTest {

  private static final String SP = "America/Sao_Paulo";
  private static final String LIS = "Europe/Lisbon";

  private final List<StopEntry> stops = new ArrayList<>();
  private final List<SegmentEntry> segments = new ArrayList<>();
  private final List<LodgingEntry> lodgings = new ArrayList<>();

  private StopEntry stop(long id, String city, String arrival, String departure) {
    StopEntry stop =
        new StopEntry(
            id,
            stops.size(),
            new StopDetails(
                null,
                city,
                "PT",
                null,
                null,
                LIS,
                LocalDate.parse(arrival),
                LocalDate.parse(departure),
                null));
    stops.add(stop);
    return stop;
  }

  private SegmentEntry segment(
      long id,
      TransportType type,
      Long origin,
      Long destination,
      String departAt,
      String departTz,
      String arriveAt,
      String arriveTz) {
    SegmentEntry segment =
        new SegmentEntry(
            id,
            new SegmentDetails(
                type,
                origin,
                destination,
                null,
                null,
                null,
                LocalDateTime.parse(departAt),
                departTz,
                arriveAt == null ? null : LocalDateTime.parse(arriveAt),
                arriveTz,
                null,
                null,
                null,
                null));
    segments.add(segment);
    return segment;
  }

  private LodgingEntry lodging(long id, StopEntry stop, String name, String in, String out) {
    LodgingEntry lodging =
        new LodgingEntry(
            id,
            stop.id(),
            null,
            new LodgingDetails(
                LodgingKind.HOTEL,
                name,
                null,
                LocalDate.parse(in),
                LocalDate.parse(out),
                null,
                null,
                null,
                null));
    lodgings.add(lodging);
    return lodging;
  }

  private List<ItineraryDay> build(String start, String end) {
    return ItineraryBuilder.build(
        new TripSnapshot(
            new TripSnapshot.TripInfo(
                1, "Portugal", parse(start), end == null ? null : LocalDate.parse(end)),
            new TripSnapshot.Owner(1, "BR", null),
            stops,
            segments,
            lodgings,
            List.of(),
            List.of()));
  }

  private static LocalDate parse(String date) {
    return date == null ? null : LocalDate.parse(date);
  }

  private static ItineraryDay day(List<ItineraryDay> days, String date) {
    return days.stream()
        .filter(d -> d.date().equals(LocalDate.parse(date)))
        .findFirst()
        .orElseThrow();
  }

  /** Casa → Lisboa (voo noturno) → Porto (trem) → casa, com hotéis em todas as noites. */
  private void portugal() {
    StopEntry lisbon = stop(10, "Lisboa", "2027-03-11", "2027-03-14");
    StopEntry porto = stop(20, "Porto", "2027-03-14", "2027-03-17");
    segment(1, TransportType.FLIGHT, null, 10L, "2027-03-10T22:40", SP, "2027-03-11T11:15", LIS);
    segment(2, TransportType.TRAIN, 10L, 20L, "2027-03-14T10:00", LIS, "2027-03-14T12:50", LIS);
    segment(3, TransportType.FLIGHT, 20L, null, "2027-03-17T12:00", LIS, "2027-03-17T19:30", SP);
    lodging(100, lisbon, "Hotel Avenida", "2027-03-11", "2027-03-14");
    lodging(200, porto, "Porto Ribeira", "2027-03-14", "2027-03-17");
  }

  @Test
  void noDatesMeansNoDays() {
    assertThat(build(null, null)).isEmpty();
  }

  @Test
  void overnightFlightIsInTransit() {
    portugal();
    List<ItineraryDay> days = build("2027-03-10", "2027-03-17");

    assertThat(days).hasSize(8);
    ItineraryDay departure = day(days, "2027-03-10");
    assertThat(departure.stops()).isEmpty();
    assertThat(departure.events())
        .singleElement()
        .satisfies(
            e -> {
              assertThat(e.kind()).isEqualTo(SEGMENT_DEPARTURE);
              assertThat(e.time()).isEqualTo(LocalTime.of(22, 40));
              assertThat(e.at().getOffset().getTotalSeconds()).isEqualTo(-3 * 3600);
            });
    assertThat(departure.overnight().kind()).isEqualTo(OvernightKind.IN_TRANSIT);
    assertThat(departure.overnight().segment().id()).isEqualTo(1);
    assertThat(departure.gap()).isFalse();

    ItineraryDay arrival = day(days, "2027-03-11");
    assertThat(arrival.stops()).extracting(s -> s.details().cityName()).containsExactly("Lisboa");
    assertThat(arrival.events())
        .extracting(ItineraryDay.Event::kind)
        .containsExactly(SEGMENT_ARRIVAL, CHECK_IN);
    assertThat(arrival.events().getFirst().time()).isEqualTo(LocalTime.of(11, 15));
    assertThat(arrival.overnight().kind()).isEqualTo(OvernightKind.LODGING);
    assertThat(arrival.overnight().lodging().details().name()).isEqualTo("Hotel Avenida");
  }

  @Test
  void cityChangeDay() {
    portugal();
    ItineraryDay change = day(build("2027-03-10", "2027-03-17"), "2027-03-14");

    assertThat(change.stops())
        .extracting(s -> s.details().cityName())
        .containsExactly("Lisboa", "Porto");
    assertThat(change.events())
        .extracting(ItineraryDay.Event::kind)
        .containsExactly(CHECK_OUT, SEGMENT, CHECK_IN);
    assertThat(change.events().get(0).lodging().details().name()).isEqualTo("Hotel Avenida");
    assertThat(change.events().get(1).segment().id()).isEqualTo(2);
    assertThat(change.events().get(2).lodging().details().name()).isEqualTo("Porto Ribeira");
    assertThat(change.overnight().lodging().details().name()).isEqualTo("Porto Ribeira");
  }

  @Test
  void lastDayIsAtHome() {
    portugal();
    ItineraryDay last = day(build("2027-03-10", "2027-03-17"), "2027-03-17");

    assertThat(last.events())
        .extracting(ItineraryDay.Event::kind)
        .containsExactly(CHECK_OUT, SEGMENT);
    assertThat(last.overnight().kind()).isEqualTo(OvernightKind.HOME);
    assertThat(last.gap()).isFalse();
  }

  @Test
  void nightWithoutLodgingIsAGap() {
    StopEntry lisbon = stop(10, "Lisboa", "2027-03-11", "2027-03-14");
    lodging(100, lisbon, "Hotel Avenida", "2027-03-11", "2027-03-13");
    stop(20, "Porto", "2027-03-15", "2027-03-17");

    List<ItineraryDay> days = build("2027-03-11", "2027-03-17");

    assertThat(days)
        .filteredOn(ItineraryDay::gap)
        .extracting(ItineraryDay::date)
        .containsExactly(
            LocalDate.parse("2027-03-13"), // Lisboa, sem hotel
            LocalDate.parse("2027-03-14"), // entre Lisboa e Porto
            LocalDate.parse("2027-03-15"),
            LocalDate.parse("2027-03-16"));
    assertThat(day(days, "2027-03-14").stops()).extracting(StopEntry::id).containsExactly(10L);
    assertThat(day(days, "2027-03-17").overnight().kind()).isEqualTo(OvernightKind.HOME);
  }

  @Test
  void eventsAreOrderedByLocalTime() {
    StopEntry lisbon = stop(10, "Lisboa", "2027-03-11", "2027-03-14");
    segment(1, TransportType.TRANSFER, null, null, "2027-03-12T18:00", LIS, null, null);
    segment(
        2, TransportType.TAXI_RIDE, null, null, "2027-03-12T09:30", LIS, "2027-03-12T09:50", LIS);
    lodging(100, lisbon, "Hotel Avenida", "2027-03-11", "2027-03-14");

    ItineraryDay day = day(build("2027-03-11", "2027-03-14"), "2027-03-12");

    assertThat(day.events()).extracting(e -> e.segment().id()).containsExactly(2L, 1L);
  }

  @Test
  void withoutEndDateGoesToTheLastItineraryDate() {
    portugal();
    List<ItineraryDay> days = build("2027-03-10", null);

    assertThat(days.getLast().date()).isEqualTo(LocalDate.parse("2027-03-17"));
  }

  @Test
  void manualDatesBeyondTheItineraryAreAtHome() {
    portugal();
    List<ItineraryDay> days = build("2027-03-09", "2027-03-18");

    assertThat(day(days, "2027-03-09").overnight().kind()).isEqualTo(OvernightKind.HOME);
    assertThat(day(days, "2027-03-18").overnight().kind()).isEqualTo(OvernightKind.HOME);
    assertThat(days).noneMatch(ItineraryDay::gap);
  }

  @Test
  void limitsTheNumberOfDays() {
    assertThat(build("2027-01-01", "2030-01-01")).hasSize(ItineraryBuilder.MAX_DAYS);
  }
}
