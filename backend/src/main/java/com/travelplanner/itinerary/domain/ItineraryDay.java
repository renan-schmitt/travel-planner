package com.travelplanner.itinerary.domain;

import com.travelplanner.itinerary.domain.TripSnapshot.ActivityEntry;
import com.travelplanner.itinerary.domain.TripSnapshot.LodgingEntry;
import com.travelplanner.itinerary.domain.TripSnapshot.SegmentEntry;
import com.travelplanner.itinerary.domain.TripSnapshot.StopEntry;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * Um dia da timeline: as paradas em que se está (até duas no dia da troca de cidade), os eventos em
 * ordem e onde se dorme.
 */
public record ItineraryDay(
    LocalDate date, List<StopEntry> stops, List<Event> events, Overnight overnight) {

  public ItineraryDay {
    stops = List.copyOf(stops);
    events = List.copyOf(events);
  }

  public boolean gap() {
    return overnight.kind() == OvernightKind.MISSING;
  }

  /**
   * Evento do dia. Trechos têm {@code time} e {@code at} (instante no fuso da ponta); check-in e
   * check-out não têm horário; atividades têm {@code time} se tiverem horário de início.
   */
  public record Event(
      ItineraryEventKind kind,
      LocalTime time,
      ZonedDateTime at,
      SegmentEntry segment,
      LodgingEntry lodging,
      ActivityEntry activity) {}

  /** Onde se dorme; {@code lodging} em {@code LODGING}, {@code segment} em {@code IN_TRANSIT}. */
  public record Overnight(OvernightKind kind, LodgingEntry lodging, SegmentEntry segment) {}
}
