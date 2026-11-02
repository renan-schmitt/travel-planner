package com.travelplanner.itinerary.domain;

import com.travelplanner.itinerary.domain.ItineraryDay.Event;
import com.travelplanner.itinerary.domain.ItineraryDay.Overnight;
import com.travelplanner.itinerary.domain.TripSnapshot.LodgingEntry;
import com.travelplanner.itinerary.domain.TripSnapshot.SegmentEntry;
import com.travelplanner.itinerary.domain.TripSnapshot.StopEntry;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Timeline dia a dia de uma viagem, de {@code startDate} a {@code endDate}. Lógica pura sobre o
 * {@link TripSnapshot}, sem efeitos colaterais.
 *
 * <ul>
 *   <li>Trechos entram no dia da data <b>local</b> de cada ponta (partida no fuso da partida,
 *       chegada no fuso da chegada).
 *   <li>Ordem dos eventos: check-outs, trechos pelo horário local, check-ins (hospedagens não têm
 *       horário; o check-out costuma ser de manhã e o check-in depois da chegada).
 *   <li>Noite: hospedagem com {@code checkIn <= dia < checkOut}; senão, trecho que parte até o dia
 *       e chega depois dele ("em trânsito"); senão, lacuna se a noite fica entre a primeira chegada
 *       e a última saída das paradas, ou "em casa" fora delas.
 * </ul>
 */
public final class ItineraryBuilder {

  /** Limite de dias (protege contra datas manuais muito distantes). */
  static final int MAX_DAYS = 366;

  private static final Comparator<Event> EVENT_ORDER =
      Comparator.comparingInt(ItineraryBuilder::group)
          .thenComparing(Event::time, Comparator.nullsFirst(Comparator.naturalOrder()))
          .thenComparing(
              e -> e.at() == null ? null : e.at().toInstant(),
              Comparator.nullsFirst(Comparator.naturalOrder()));

  private ItineraryBuilder() {}

  public static List<ItineraryDay> build(TripSnapshot snapshot) {
    LocalDate start = snapshot.trip().startDate();
    if (start == null) {
      return List.of();
    }
    LocalDate end = snapshot.trip().endDate();
    if (end == null) {
      end = lastItineraryDate(snapshot, start);
    }
    LocalDate firstArrival =
        snapshot.stops().stream()
            .map(s -> s.details().arrivalDate())
            .min(Comparator.naturalOrder())
            .orElse(null);
    LocalDate lastDeparture =
        snapshot.stops().stream()
            .map(s -> s.details().departureDate())
            .max(Comparator.naturalOrder())
            .orElse(null);
    List<ItineraryDay> days = new ArrayList<>();
    for (LocalDate date = start;
        !date.isAfter(end) && days.size() < MAX_DAYS;
        date = date.plusDays(1)) {
      days.add(
          new ItineraryDay(
              date,
              stopsOn(snapshot, date),
              eventsOn(snapshot, date),
              overnight(snapshot, date, firstArrival, lastDeparture)));
    }
    return days;
  }

  private static List<StopEntry> stopsOn(TripSnapshot snapshot, LocalDate date) {
    return snapshot.stops().stream()
        .filter(
            s ->
                !date.isBefore(s.details().arrivalDate())
                    && !date.isAfter(s.details().departureDate()))
        .sorted(
            Comparator.comparing((StopEntry s) -> s.details().arrivalDate())
                .thenComparingInt(StopEntry::seq))
        .toList();
  }

  private static List<Event> eventsOn(TripSnapshot snapshot, LocalDate date) {
    List<Event> events = new ArrayList<>();
    for (LodgingEntry lodging : snapshot.lodgings()) {
      if (lodging.details().checkOut().equals(date)) {
        events.add(new Event(ItineraryEventKind.CHECK_OUT, null, null, null, lodging));
      }
      if (lodging.details().checkIn().equals(date)) {
        events.add(new Event(ItineraryEventKind.CHECK_IN, null, null, null, lodging));
      }
    }
    for (SegmentEntry segment : snapshot.segments()) {
      SegmentDetails d = segment.details();
      LocalDate departDate = d.departAt().toLocalDate();
      LocalDate arriveDate = d.arriveAt() == null ? null : d.arriveAt().toLocalDate();
      if (arriveDate == null || arriveDate.equals(departDate)) {
        if (departDate.equals(date)) {
          events.add(segmentEvent(ItineraryEventKind.SEGMENT, d.departure(), segment));
        }
        continue;
      }
      if (departDate.equals(date)) {
        events.add(segmentEvent(ItineraryEventKind.SEGMENT_DEPARTURE, d.departure(), segment));
      }
      if (arriveDate.equals(date)) {
        events.add(segmentEvent(ItineraryEventKind.SEGMENT_ARRIVAL, d.arrival(), segment));
      }
    }
    events.sort(EVENT_ORDER);
    return events;
  }

  private static Event segmentEvent(
      ItineraryEventKind kind, ZonedDateTime at, SegmentEntry segment) {
    return new Event(kind, at.toLocalTime(), at, segment, null);
  }

  private static int group(Event event) {
    return switch (event.kind()) {
      case CHECK_OUT -> 0;
      case SEGMENT, SEGMENT_DEPARTURE, SEGMENT_ARRIVAL -> 1;
      case CHECK_IN -> 2;
    };
  }

  private static Overnight overnight(
      TripSnapshot snapshot, LocalDate date, LocalDate firstArrival, LocalDate lastDeparture) {
    Optional<LodgingEntry> lodging =
        snapshot.lodgings().stream()
            .filter(
                l -> !date.isBefore(l.details().checkIn()) && date.isBefore(l.details().checkOut()))
            .findFirst();
    if (lodging.isPresent()) {
      return new Overnight(OvernightKind.LODGING, lodging.get(), null);
    }
    Optional<SegmentEntry> transit =
        snapshot.segments().stream()
            .filter(
                s ->
                    !s.details().departAt().toLocalDate().isAfter(date)
                        && s.details().arriveAt() != null
                        && s.details().arriveAt().toLocalDate().isAfter(date))
            .findFirst();
    if (transit.isPresent()) {
      return new Overnight(OvernightKind.IN_TRANSIT, null, transit.get());
    }
    boolean withinStops =
        firstArrival != null && !date.isBefore(firstArrival) && date.isBefore(lastDeparture);
    return new Overnight(withinStops ? OvernightKind.MISSING : OvernightKind.HOME, null, null);
  }

  /** Sem data de fim: a última data do roteiro (saída de parada ou ponta de trecho). */
  private static LocalDate lastItineraryDate(TripSnapshot snapshot, LocalDate start) {
    LocalDate last = start;
    for (StopEntry stop : snapshot.stops()) {
      last = latest(last, stop.details().departureDate());
    }
    for (SegmentEntry segment : snapshot.segments()) {
      LocalDateTime end =
          segment.details().arriveAt() != null
              ? segment.details().arriveAt()
              : segment.details().departAt();
      last = latest(last, end.toLocalDate());
    }
    return last;
  }

  private static LocalDate latest(LocalDate a, LocalDate b) {
    return b.isAfter(a) ? b : a;
  }
}
