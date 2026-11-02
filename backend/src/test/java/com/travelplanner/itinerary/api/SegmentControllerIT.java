package com.travelplanner.itinerary.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.travelplanner.IntegrationTest;
import com.travelplanner.auth.TestUsers;
import com.travelplanner.auth.domain.AppUser;
import com.travelplanner.auth.domain.AppUserService;
import com.travelplanner.trip.domain.TripChangedEvent;
import com.travelplanner.trip.domain.TripMember;
import com.travelplanner.trip.domain.TripRole;
import com.travelplanner.trip.infra.TripMemberRepository;
import com.travelplanner.trip.infra.TripRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@RecordApplicationEvents
class SegmentControllerIT extends IntegrationTest {

  @Autowired MockMvc mvc;
  @Autowired AppUserService users;
  @Autowired TripRepository trips;
  @Autowired TripMemberRepository members;
  @Autowired ApplicationEvents events;

  private record User(long id, RequestPostProcessor session) {}

  private User newUser() {
    var google = TestUsers.googleUser(UUID.randomUUID() + "@example.com", "Ana");
    AppUser user = users.upsertFromOidc(google);
    return new User(user.getId(), TestUsers.loggedIn(users, google));
  }

  private ResultActions send(User user, MockHttpServletRequestBuilder request, String json)
      throws Exception {
    return mvc.perform(
        request
            .with(user.session())
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(json));
  }

  private static long id(ResultActions result) throws Exception {
    String body = result.andReturn().getResponse().getContentAsString();
    return ((Number) JsonPath.read(body, "$.id")).longValue();
  }

  private long createTrip(User user, String datesJson) throws Exception {
    return id(
        send(
                user,
                post("/api/v1/trips"),
                "{\"name\": \"Portugal\", \"status\": \"PLANNING\"%s}".formatted(datesJson))
            .andExpect(status().isCreated()));
  }

  private long addStop(User user, long tripId, String city, String arrival, String departure)
      throws Exception {
    String json =
        """
        {"cityName": "%s", "countryCode": "PT", "timezone": "Europe/Lisbon",
         "arrivalDate": "%s", "departureDate": "%s"}
        """
            .formatted(city, arrival, departure);
    return id(
        send(user, post("/api/v1/trips/{t}/stops", tripId), json).andExpect(status().isCreated()));
  }

  private static String json(Long origin, Long destination, Long local, String extra) {
    return """
        {"type": "FLIGHT", "originStopId": %s, "destinationStopId": %s, "localStopId": %s %s}
        """
        .formatted(origin, destination, local, extra);
  }

  /** GRU 10/03 22:40 (GMT-3) → LIS 11/03 11:15 (GMT+0). */
  private static String outboundFlight(long lisbon) {
    return json(
        null,
        lisbon,
        null,
        """
        , "originLabel": "GRU", "destinationLabel": "LIS",
          "departAt": "2027-03-10T22:40", "departTz": "America/Sao_Paulo",
          "arriveAt": "2027-03-11T11:15", "arriveTz": "Europe/Lisbon",
          "carrier": " TAP ", "code": "TP 88", "bookingRef": "ABC123", "notes": " "
        """);
  }

  private long addSegment(User user, long tripId, String json) throws Exception {
    return id(
        send(user, post("/api/v1/trips/{t}/segments", tripId), json)
            .andExpect(status().isCreated()));
  }

  private void addMember(long tripId, User user, TripRole role) {
    members.save(
        TripMember.of(trips.findById(tripId).orElseThrow(), user.id(), role, Instant.now()));
  }

  @Test
  void roundTripWithOvernightFlightAndDerivedTripDates() throws Exception {
    User ana = newUser();
    long tripId = createTrip(ana, "");
    long lisbon = addStop(ana, tripId, "Lisboa", "2027-03-11", "2027-03-14");
    long porto = addStop(ana, tripId, "Porto", "2027-03-14", "2027-03-17");

    send(ana, post("/api/v1/trips/{t}/segments", tripId), outboundFlight(lisbon))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.type").value("FLIGHT"))
        .andExpect(jsonPath("$.local").value(false))
        .andExpect(jsonPath("$.originStopId").doesNotExist())
        .andExpect(jsonPath("$.destinationStopId").value(lisbon))
        .andExpect(jsonPath("$.departAt").value("2027-03-10T22:40:00"))
        .andExpect(jsonPath("$.departInstant").value("2027-03-10T22:40:00-03:00"))
        .andExpect(jsonPath("$.arriveInstant").value("2027-03-11T11:15:00Z"))
        .andExpect(jsonPath("$.carrier").value("TAP"))
        .andExpect(jsonPath("$.notes").doesNotExist())
        .andExpect(jsonPath("$.version").value(0));
    // Trem Lisboa → Porto, sem horário de chegada.
    addSegment(
        ana,
        tripId,
        """
        {"type": "TRAIN", "originStopId": %d, "destinationStopId": %d,
         "departAt": "2027-03-14T09:00", "departTz": "Europe/Lisbon"}
        """
            .formatted(lisbon, porto));
    // Volta OPO → GRU, chegando 18/03 no relógio de São Paulo.
    addSegment(
        ana,
        tripId,
        json(
            porto,
            null,
            null,
            """
            , "departAt": "2027-03-17T23:30", "departTz": "Europe/Lisbon",
              "arriveAt": "2027-03-18T06:40", "arriveTz": "America/Sao_Paulo"
            """));
    // Transfer local em Lisboa.
    addSegment(
        ana,
        tripId,
        """
        {"type": "TRANSFER", "localStopId": %d, "originLabel": "Aeroporto",
         "destinationLabel": "Hotel", "departAt": "2027-03-11T12:00", "departTz": "Europe/Lisbon"}
        """
            .formatted(lisbon));

    mvc.perform(get("/api/v1/trips/{t}/segments", tripId).with(ana.session()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(4))
        .andExpect(jsonPath("$[0].type").value("FLIGHT"))
        .andExpect(jsonPath("$[1].type").value("TRANSFER"))
        .andExpect(jsonPath("$[1].local").value(true))
        .andExpect(jsonPath("$[2].type").value("TRAIN"))
        .andExpect(jsonPath("$[2].arriveAt").doesNotExist())
        .andExpect(jsonPath("$[3].destinationStopId").doesNotExist());

    // Período da viagem: partida em 10/03 (antes da 1ª parada) até a chegada em 18/03.
    mvc.perform(get("/api/v1/trips/{t}", tripId).with(ana.session()))
        .andExpect(jsonPath("$.startDate").value("2027-03-10"))
        .andExpect(jsonPath("$.endDate").value("2027-03-18"))
        .andExpect(jsonPath("$.datesFromStops").value(true));

    assertThat(events.stream(TripChangedEvent.class)).contains(new TripChangedEvent(tripId));
  }

  @Test
  void manualTripDatesAreNotChangedBySegments() throws Exception {
    User ana = newUser();
    long tripId = createTrip(ana, ", \"startDate\": \"2027-03-11\", \"endDate\": \"2027-03-17\"");
    long lisbon = addStop(ana, tripId, "Lisboa", "2027-03-11", "2027-03-14");
    addSegment(ana, tripId, outboundFlight(lisbon));

    mvc.perform(get("/api/v1/trips/{t}", tripId).with(ana.session()))
        .andExpect(jsonPath("$.startDate").value("2027-03-11"))
        .andExpect(jsonPath("$.datesFromStops").value(false));
  }

  @Test
  void updateDeleteAndConflict() throws Exception {
    User ana = newUser();
    long tripId = createTrip(ana, "");
    long lisbon = addStop(ana, tripId, "Lisboa", "2027-03-11", "2027-03-14");
    long segmentId = addSegment(ana, tripId, outboundFlight(lisbon));

    send(
            ana,
            put("/api/v1/trips/{t}/segments/{s}", tripId, segmentId),
            json(
                null,
                lisbon,
                null,
                """
                , "version": 0, "departAt": "2027-03-09T22:40", "departTz": "America/Sao_Paulo"
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.version").value(1))
        .andExpect(jsonPath("$.arriveAt").doesNotExist())
        .andExpect(jsonPath("$.carrier").doesNotExist());
    mvc.perform(get("/api/v1/trips/{t}", tripId).with(ana.session()))
        .andExpect(jsonPath("$.startDate").value("2027-03-09"));

    send(
            ana,
            put("/api/v1/trips/{t}/segments/{s}", tripId, segmentId),
            json(
                null,
                lisbon,
                null,
                """
                , "version": 0, "departAt": "2027-03-09T22:40", "departTz": "America/Sao_Paulo"
                """))
        .andExpect(status().isConflict());

    mvc.perform(
            delete("/api/v1/trips/{t}/segments/{s}", tripId, segmentId)
                .with(ana.session())
                .with(csrf()))
        .andExpect(status().isNoContent());
    mvc.perform(get("/api/v1/trips/{t}", tripId).with(ana.session()))
        .andExpect(jsonPath("$.startDate").value("2027-03-11"));
  }

  @Test
  void validation() throws Exception {
    User ana = newUser();
    long tripId = createTrip(ana, "");
    long lisbon = addStop(ana, tripId, "Lisboa", "2027-03-11", "2027-03-14");
    long otherTrip = createTrip(ana, "");
    long otherStop = addStop(ana, otherTrip, "Faro", "2027-05-01", "2027-05-03");
    String times = ", \"departAt\": \"2027-03-14T10:00\", \"departTz\": \"Europe/Lisbon\"";

    // Chega antes de partir (Lisboa 10:00 → Roma 10:30 = 09:30 em Lisboa).
    send(
            ana,
            post("/api/v1/trips/{t}/segments", tripId),
            json(
                lisbon,
                null,
                null,
                times + ", \"arriveAt\": \"2027-03-14T10:30\", \"arriveTz\": \"Europe/Rome\""))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("arriveAt"));
    // Local e deslocamento ao mesmo tempo.
    send(ana, post("/api/v1/trips/{t}/segments", tripId), json(lisbon, null, lisbon, times))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("localStopId"));
    // Casa → casa.
    send(ana, post("/api/v1/trips/{t}/segments", tripId), json(null, null, null, times))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("destinationStopId"));
    // Parada de outra viagem.
    send(ana, post("/api/v1/trips/{t}/segments", tripId), json(otherStop, null, null, times))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("originStopId"));
    // Fuso inválido e partida ausente.
    send(
            ana,
            post("/api/v1/trips/{t}/segments", tripId),
            json(lisbon, null, null, ", \"departTz\": \"Marte/Base\""))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.length()").value(2));
  }

  @Test
  void deletingAStopRemovesItsSegments() throws Exception {
    User ana = newUser();
    long tripId = createTrip(ana, "");
    long lisbon = addStop(ana, tripId, "Lisboa", "2027-03-11", "2027-03-14");
    addSegment(ana, tripId, outboundFlight(lisbon));

    mvc.perform(
            delete("/api/v1/trips/{t}/stops/{s}", tripId, lisbon).with(ana.session()).with(csrf()))
        .andExpect(status().isNoContent());
    mvc.perform(get("/api/v1/trips/{t}/segments", tripId).with(ana.session()))
        .andExpect(jsonPath("$.length()").value(0));
    mvc.perform(get("/api/v1/trips/{t}", tripId).with(ana.session()))
        .andExpect(jsonPath("$.startDate").doesNotExist());
  }

  @Test
  void accessByRole() throws Exception {
    User owner = newUser();
    User viewer = newUser();
    User editor = newUser();
    User stranger = newUser();
    long tripId = createTrip(owner, "");
    long lisbon = addStop(owner, tripId, "Lisboa", "2027-03-11", "2027-03-14");
    long segmentId = addSegment(owner, tripId, outboundFlight(lisbon));
    addMember(tripId, viewer, TripRole.VIEWER);
    addMember(tripId, editor, TripRole.EDITOR);

    mvc.perform(get("/api/v1/trips/{t}/segments", tripId).with(viewer.session()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1));
    send(viewer, post("/api/v1/trips/{t}/segments", tripId), outboundFlight(lisbon))
        .andExpect(status().isForbidden());
    mvc.perform(
            delete("/api/v1/trips/{t}/segments/{s}", tripId, segmentId)
                .with(viewer.session())
                .with(csrf()))
        .andExpect(status().isForbidden());

    addSegment(editor, tripId, outboundFlight(lisbon));

    mvc.perform(get("/api/v1/trips/{t}/segments", tripId).with(stranger.session()))
        .andExpect(status().isNotFound());

    long otherTrip = createTrip(owner, "");
    mvc.perform(
            delete("/api/v1/trips/{t}/segments/{s}", otherTrip, segmentId)
                .with(owner.session())
                .with(csrf()))
        .andExpect(status().isNotFound());
  }
}
