package com.travelplanner.itinerary.api;

import com.travelplanner.itinerary.domain.ItineraryDay;
import com.travelplanner.itinerary.domain.ItineraryEventKind;
import com.travelplanner.itinerary.domain.ItineraryService.Itinerary;
import com.travelplanner.itinerary.domain.LodgingKind;
import com.travelplanner.itinerary.domain.OvernightKind;
import com.travelplanner.itinerary.domain.SegmentDetails;
import com.travelplanner.itinerary.domain.TransportType;
import com.travelplanner.itinerary.domain.TripSnapshot;
import com.travelplanner.itinerary.domain.TripSnapshot.LodgingEntry;
import com.travelplanner.itinerary.domain.TripSnapshot.SegmentEntry;
import com.travelplanner.itinerary.domain.TripSnapshot.StopEntry;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Timeline dia a dia da viagem (vazia enquanto a viagem não tem datas). */
public record ItineraryResponse(@NotNull List<ItineraryDayResponse> days) {

  private static final DateTimeFormatter HOUR = DateTimeFormatter.ofPattern("HH:mm");

  /**
   * Um dia: paradas em que se está (até duas no dia da troca), eventos em ordem e onde se dorme.
   * {@code gap} = noite sem hospedagem dentro do roteiro.
   */
  public record ItineraryDayResponse(
      @NotNull LocalDate date,
      @NotNull List<ItineraryStopResponse> stops,
      @NotNull List<ItineraryEventResponse> events,
      @NotNull OvernightResponse overnight,
      @NotNull Boolean gap) {}

  public record ItineraryStopResponse(
      @NotNull Long id,
      @NotNull String cityName,
      @NotNull String countryCode,
      @NotNull String timezone,
      @NotNull LocalDate arrivalDate,
      @NotNull LocalDate departureDate) {}

  /**
   * Evento do dia. Trechos têm {@code time} ("22:40", relógio local da ponta) e {@code instant}
   * (com o offset da ponta); check-in e check-out não têm horário.
   */
  public record ItineraryEventResponse(
      @NotNull ItineraryEventKind kind,
      String time,
      OffsetDateTime instant,
      ItinerarySegmentResponse segment,
      ItineraryLodgingResponse lodging) {}

  /**
   * Trecho resumido. {@code origin}/{@code destination}: rótulo digitado ou cidade da parada; nulo
   * = casa (em trechos locais, nulo = sem rótulo, e {@code localCity} é a cidade da parada).
   */
  public record ItinerarySegmentResponse(
      @NotNull Long id,
      @NotNull TransportType type,
      @NotNull Boolean local,
      String origin,
      String destination,
      String localCity,
      @NotNull LocalDateTime departAt,
      @NotNull OffsetDateTime departInstant,
      LocalDateTime arriveAt,
      OffsetDateTime arriveInstant,
      String carrier,
      String code,
      String bookingRef) {}

  public record ItineraryLodgingResponse(
      @NotNull Long id,
      @NotNull Long stopId,
      @NotNull LodgingKind kind,
      @NotNull String name,
      String address,
      @NotNull LocalDate checkIn,
      @NotNull LocalDate checkOut) {}

  /** Onde se dorme: {@code lodging} em {@code LODGING}, {@code segment} em {@code IN_TRANSIT}. */
  public record OvernightResponse(
      @NotNull OvernightKind kind,
      ItineraryLodgingResponse lodging,
      ItinerarySegmentResponse segment) {}

  static ItineraryResponse from(Itinerary itinerary) {
    TripSnapshot snapshot = itinerary.snapshot();
    return new ItineraryResponse(itinerary.days().stream().map(day -> day(snapshot, day)).toList());
  }

  private static ItineraryDayResponse day(TripSnapshot snapshot, ItineraryDay day) {
    return new ItineraryDayResponse(
        day.date(),
        day.stops().stream().map(ItineraryResponse::stop).toList(),
        day.events().stream()
            .map(
                e ->
                    new ItineraryEventResponse(
                        e.kind(),
                        e.time() == null ? null : HOUR.format(e.time()),
                        e.at() == null ? null : e.at().toOffsetDateTime(),
                        segment(snapshot, e.segment()),
                        lodging(e.lodging())))
            .toList(),
        new OvernightResponse(
            day.overnight().kind(),
            lodging(day.overnight().lodging()),
            segment(snapshot, day.overnight().segment())),
        day.gap());
  }

  private static ItineraryStopResponse stop(StopEntry stop) {
    var d = stop.details();
    return new ItineraryStopResponse(
        stop.id(), d.cityName(), d.countryCode(), d.timezone(), d.arrivalDate(), d.departureDate());
  }

  private static ItinerarySegmentResponse segment(TripSnapshot snapshot, SegmentEntry segment) {
    if (segment == null) {
      return null;
    }
    SegmentDetails d = segment.details();
    return new ItinerarySegmentResponse(
        segment.id(),
        d.type(),
        d.local(),
        endpoint(snapshot, d.originLabel(), d.originStopId()),
        endpoint(snapshot, d.destinationLabel(), d.destinationStopId()),
        snapshot.stop(d.localStopId()).map(s -> s.details().cityName()).orElse(null),
        d.departAt(),
        d.departure().toOffsetDateTime(),
        d.arriveAt(),
        d.arriveAt() == null ? null : d.arrival().toOffsetDateTime(),
        d.carrier(),
        d.code(),
        d.bookingRef());
  }

  private static String endpoint(TripSnapshot snapshot, String label, Long stopId) {
    if (label != null) {
      return label;
    }
    return snapshot.stop(stopId).map(s -> s.details().cityName()).orElse(null);
  }

  private static ItineraryLodgingResponse lodging(LodgingEntry lodging) {
    if (lodging == null) {
      return null;
    }
    var d = lodging.details();
    return new ItineraryLodgingResponse(
        lodging.id(), lodging.stopId(), d.kind(), d.name(), d.address(), d.checkIn(), d.checkOut());
  }
}
