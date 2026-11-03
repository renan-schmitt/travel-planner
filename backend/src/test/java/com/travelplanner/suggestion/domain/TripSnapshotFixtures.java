package com.travelplanner.suggestion.domain;

import com.travelplanner.auth.domain.HomeCity;
import com.travelplanner.checklist.domain.ChecklistStatus;
import com.travelplanner.itinerary.domain.LodgingDetails;
import com.travelplanner.itinerary.domain.LodgingKind;
import com.travelplanner.itinerary.domain.SegmentDetails;
import com.travelplanner.itinerary.domain.StopDetails;
import com.travelplanner.itinerary.domain.TransportType;
import com.travelplanner.itinerary.domain.TripSnapshot;
import com.travelplanner.itinerary.domain.TripSnapshot.LodgingEntry;
import com.travelplanner.itinerary.domain.TripSnapshot.SegmentEntry;
import com.travelplanner.itinerary.domain.TripSnapshot.StopEntry;
import com.travelplanner.itinerary.domain.TripSnapshot.TravelerEntry;
import com.travelplanner.trip.domain.AgeGroup;
import com.travelplanner.trip.domain.TravelerDetails;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * Builder de {@link TripSnapshot}/{@link GapContext} para os testes das regras. Casa padrão: São
 * Paulo (BR). Datas da viagem: da primeira chegada à última saída das paradas, a não ser que sejam
 * dadas com {@link #dates}.
 */
public class TripSnapshotFixtures {

  public static final String SP = "America/Sao_Paulo";
  public static final String LIS = "Europe/Lisbon";

  private long nextId = 100;
  private HomeCity home = new HomeCity("São Paulo", null, "BR", SP);
  private String nationality = "BR";
  private LocalDate start;
  private LocalDate end;
  private Instant now = Instant.parse("2027-01-01T12:00:00Z");
  private final List<StopEntry> stops = new ArrayList<>();
  private final List<SegmentEntry> segments = new ArrayList<>();
  private final List<LodgingEntry> lodgings = new ArrayList<>();
  private final List<TravelerEntry> travelers = new ArrayList<>();
  private final List<GapContext.ChecklistEntry> checklist = new ArrayList<>();

  public static TripSnapshotFixtures trip() {
    return new TripSnapshotFixtures();
  }

  public TripSnapshotFixtures home(String city, String countryCode, String timezone) {
    this.home = new HomeCity(city, null, countryCode, timezone);
    return this;
  }

  public TripSnapshotFixtures noHome(String nationality) {
    this.home = null;
    this.nationality = nationality;
    return this;
  }

  public TripSnapshotFixtures dates(String start, String end) {
    this.start = start == null ? null : LocalDate.parse(start);
    this.end = end == null ? null : LocalDate.parse(end);
    return this;
  }

  public TripSnapshotFixtures now(String instant) {
    this.now = Instant.parse(instant);
    return this;
  }

  public StopEntry stop(String city, String country, String tz, String arrival, String departure) {
    StopEntry stop =
        new StopEntry(
            nextId++,
            stops.size(),
            new StopDetails(
                null,
                city,
                country,
                null,
                null,
                tz,
                LocalDate.parse(arrival),
                LocalDate.parse(departure),
                null));
    stops.add(stop);
    return stop;
  }

  /** Parada em Portugal (fuso de Lisboa). */
  public StopEntry stop(String city, String arrival, String departure) {
    return stop(city, "PT", LIS, arrival, departure);
  }

  /** Trecho entre {@code origin} e {@code destination} ({@code null} = casa). */
  public SegmentEntry segment(
      TransportType type,
      StopEntry origin,
      StopEntry destination,
      String departAt,
      String departTz,
      String arriveAt,
      String arriveTz) {
    return add(
        type,
        origin == null ? null : origin.id(),
        destination == null ? null : destination.id(),
        null,
        departAt,
        departTz,
        arriveAt,
        arriveTz);
  }

  public SegmentEntry flight(
      StopEntry origin,
      StopEntry destination,
      String departAt,
      String departTz,
      String arriveAt,
      String arriveTz) {
    return segment(
        TransportType.FLIGHT, origin, destination, departAt, departTz, arriveAt, arriveTz);
  }

  /** Trecho local (transfer, aluguel de carro) dentro de uma parada, no fuso dela. */
  public SegmentEntry local(TransportType type, StopEntry stop, String departAt, String arriveAt) {
    String tz = stop.details().timezone();
    return add(type, null, null, stop.id(), departAt, tz, arriveAt, arriveAt == null ? null : tz);
  }

  private SegmentEntry add(
      TransportType type,
      Long origin,
      Long destination,
      Long local,
      String departAt,
      String departTz,
      String arriveAt,
      String arriveTz) {
    SegmentEntry segment =
        new SegmentEntry(
            nextId++,
            new SegmentDetails(
                type,
                origin,
                destination,
                local,
                null,
                null,
                LocalDateTime.parse(departAt),
                departTz,
                arriveAt == null ? null : LocalDateTime.parse(arriveAt),
                arriveTz,
                type == TransportType.FLIGHT ? "TAP" : null,
                type == TransportType.FLIGHT ? "TP" + nextId : null,
                null,
                null));
    segments.add(segment);
    return segment;
  }

  public LodgingEntry lodging(StopEntry stop, String checkIn, String checkOut) {
    LodgingEntry lodging =
        new LodgingEntry(
            nextId++,
            stop.id(),
            new LodgingDetails(
                LodgingKind.HOTEL,
                "Hotel " + stop.details().cityName(),
                null,
                LocalDate.parse(checkIn),
                LocalDate.parse(checkOut),
                null,
                null,
                null,
                null));
    lodgings.add(lodging);
    return lodging;
  }

  public TravelerEntry traveler(String name, String passportExpiry, String... nationalities) {
    TravelerEntry traveler =
        new TravelerEntry(
            nextId++,
            new TravelerDetails(
                name,
                AgeGroup.ADULT,
                null,
                List.of(nationalities.length == 0 ? new String[] {"BR"} : nationalities),
                passportExpiry == null ? null : LocalDate.parse(passportExpiry)));
    travelers.add(traveler);
    return traveler;
  }

  public TripSnapshotFixtures checklistItem(String ruleCode, String title) {
    checklist.add(
        new GapContext.ChecklistEntry(
            ruleCode, ruleCode == null ? null : "k", title, ChecklistStatus.OPEN));
    return this;
  }

  public TripSnapshot snapshot() {
    LocalDate s =
        start != null
            ? start
            : stops.stream()
                .map(e -> e.details().arrivalDate())
                .min(LocalDate::compareTo)
                .orElse(null);
    LocalDate e =
        end != null
            ? end
            : stops.stream()
                .map(x -> x.details().departureDate())
                .max(LocalDate::compareTo)
                .orElse(null);
    return new TripSnapshot(
        new TripSnapshot.TripInfo(1L, "Viagem", s, e),
        new TripSnapshot.Owner(1L, nationality, home),
        stops,
        segments.stream()
            .sorted(
                java.util.Comparator.comparing(
                    (SegmentEntry x) -> x.details().departure().toInstant()))
            .toList(),
        lodgings,
        travelers);
  }

  public GapContext context() {
    return new GapContext(snapshot(), Clock.fixed(now, ZoneOffset.UTC), checklist);
  }
}
