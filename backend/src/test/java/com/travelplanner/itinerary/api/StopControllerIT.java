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
class StopControllerIT extends IntegrationTest {

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

  private static String stop(String city, String country, String tz, String arrival, String dep) {
    return """
        {"cityName": "%s", "countryCode": "%s", "timezone": "%s",
         "arrivalDate": "%s", "departureDate": "%s"}
        """
        .formatted(city, country, tz, arrival, dep);
  }

  private static String lisbon() {
    return """
        {"cityPlaceId": "ChIJLisboa", "cityName": "Lisboa", "countryCode": "PT",
         "lat": 38.7222524, "lng": -9.1393366, "timezone": "Europe/Lisbon",
         "arrivalDate": "2027-03-10", "departureDate": "2027-03-14", "notes": "  Alfama  "}
        """;
  }

  private static String porto() {
    return stop("Porto", "PT", "Europe/Lisbon", "2027-03-14", "2027-03-17");
  }

  private long addStop(User user, long tripId, String json) throws Exception {
    return id(
        send(user, post("/api/v1/trips/{t}/stops", tripId), json).andExpect(status().isCreated()));
  }

  private void addMember(long tripId, User user, TripRole role) {
    members.save(
        TripMember.of(trips.findById(tripId).orElseThrow(), user.id(), role, Instant.now()));
  }

  @Test
  void lisbonThenPortoWithTimezonesAndDerivedTripDates() throws Exception {
    User ana = newUser();
    long tripId = createTrip(ana, "");

    send(ana, post("/api/v1/trips/{t}/stops", tripId), lisbon())
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.seq").value(1))
        .andExpect(jsonPath("$.cityPlaceId").value("ChIJLisboa"))
        .andExpect(jsonPath("$.lat").value(38.722252))
        .andExpect(jsonPath("$.timezone").value("Europe/Lisbon"))
        .andExpect(jsonPath("$.nights").value(4))
        .andExpect(jsonPath("$.notes").value("Alfama"))
        .andExpect(jsonPath("$.version").value(0));
    addStop(ana, tripId, porto());

    mvc.perform(get("/api/v1/trips/{t}/stops", tripId).with(ana.session()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].cityName").value("Lisboa"))
        .andExpect(jsonPath("$[1].cityName").value("Porto"))
        .andExpect(jsonPath("$[1].seq").value(2))
        .andExpect(jsonPath("$[1].nights").value(3))
        .andExpect(jsonPath("$[1].cityPlaceId").doesNotExist());

    mvc.perform(get("/api/v1/trips/{t}", tripId).with(ana.session()))
        .andExpect(jsonPath("$.startDate").value("2027-03-10"))
        .andExpect(jsonPath("$.endDate").value("2027-03-17"))
        .andExpect(jsonPath("$.datesFromStops").value(true))
        .andExpect(jsonPath("$.destinationCount").value(2));
    mvc.perform(get("/api/v1/trips").with(ana.session()))
        .andExpect(jsonPath("$[0].destinationCount").value(2));

    assertThat(events.stream(TripChangedEvent.class)).contains(new TripChangedEvent(tripId));
  }

  @Test
  void newStopIsInsertedInChronologicalOrder() throws Exception {
    User ana = newUser();
    long tripId = createTrip(ana, "");
    addStop(ana, tripId, porto());
    addStop(ana, tripId, stop("Évora", "PT", "Europe/Lisbon", "2027-03-20", "2027-03-21"));
    addStop(ana, tripId, lisbon());

    mvc.perform(get("/api/v1/trips/{t}/stops", tripId).with(ana.session()))
        .andExpect(jsonPath("$[0].cityName").value("Lisboa"))
        .andExpect(jsonPath("$[1].cityName").value("Porto"))
        .andExpect(jsonPath("$[2].cityName").value("Évora"))
        .andExpect(jsonPath("$[2].seq").value(3));
  }

  @Test
  void manualTripDatesAreKeptUntilCleared() throws Exception {
    User ana = newUser();
    long tripId = createTrip(ana, ", \"startDate\": \"2027-03-01\", \"endDate\": \"2027-03-31\"");
    addStop(ana, tripId, lisbon());

    mvc.perform(get("/api/v1/trips/{t}", tripId).with(ana.session()))
        .andExpect(jsonPath("$.startDate").value("2027-03-01"))
        .andExpect(jsonPath("$.datesFromStops").value(false));

    String version =
        JsonPath.read(
                mvc.perform(get("/api/v1/trips/{t}", tripId).with(ana.session()))
                    .andReturn()
                    .getResponse()
                    .getContentAsString(),
                "$.version")
            .toString();
    send(
            ana,
            put("/api/v1/trips/{t}", tripId),
            "{\"version\": %s, \"name\": \"Portugal\", \"status\": \"PLANNING\"}"
                .formatted(version))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.startDate").value("2027-03-10"))
        .andExpect(jsonPath("$.endDate").value("2027-03-14"))
        .andExpect(jsonPath("$.datesFromStops").value(true));
  }

  @Test
  void deletingStopsRecalculatesTripDates() throws Exception {
    User ana = newUser();
    long tripId = createTrip(ana, "");
    long lisbonId = addStop(ana, tripId, lisbon());
    long portoId = addStop(ana, tripId, porto());

    mvc.perform(
            delete("/api/v1/trips/{t}/stops/{s}", tripId, lisbonId)
                .with(ana.session())
                .with(csrf()))
        .andExpect(status().isNoContent());
    mvc.perform(get("/api/v1/trips/{t}", tripId).with(ana.session()))
        .andExpect(jsonPath("$.startDate").value("2027-03-14"))
        .andExpect(jsonPath("$.destinationCount").value(1));

    mvc.perform(
            delete("/api/v1/trips/{t}/stops/{s}", tripId, portoId).with(ana.session()).with(csrf()))
        .andExpect(status().isNoContent());
    mvc.perform(get("/api/v1/trips/{t}", tripId).with(ana.session()))
        .andExpect(jsonPath("$.startDate").doesNotExist())
        .andExpect(jsonPath("$.endDate").doesNotExist());
  }

  @Test
  void updateWithVersionAndConflict() throws Exception {
    User ana = newUser();
    long tripId = createTrip(ana, "");
    long stopId = addStop(ana, tripId, porto());

    String body =
        """
        {"version": 0, "cityName": "Porto", "countryCode": "PT", "timezone": "Europe/Lisbon",
         "arrivalDate": "2027-03-14", "departureDate": "2027-03-18", "notes": "Ribeira"}
        """;
    send(ana, put("/api/v1/trips/{t}/stops/{s}", tripId, stopId), body)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nights").value(4))
        .andExpect(jsonPath("$.notes").value("Ribeira"))
        .andExpect(jsonPath("$.version").value(1));
    send(ana, put("/api/v1/trips/{t}/stops/{s}", tripId, stopId), body)
        .andExpect(status().isConflict());

    mvc.perform(get("/api/v1/trips/{t}", tripId).with(ana.session()))
        .andExpect(jsonPath("$.endDate").value("2027-03-18"));
  }

  @Test
  void overlappingStopsAreRejected() throws Exception {
    User ana = newUser();
    long tripId = createTrip(ana, "");
    addStop(ana, tripId, lisbon());

    send(
            ana,
            post("/api/v1/trips/{t}/stops", tripId),
            stop("Sintra", "PT", "Europe/Lisbon", "2027-03-12", "2027-03-16"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("arrivalDate"))
        .andExpect(
            jsonPath("$.errors[0].message")
                .value("As datas se sobrepõem à parada em Lisboa (10/03 a 14/03)."));

    // Bate-volta dentro de outra parada divide só um dia: é permitido.
    addStop(ana, tripId, stop("Sintra", "PT", "Europe/Lisbon", "2027-03-12", "2027-03-12"));
  }

  @Test
  void editingAStopDoesNotOverlapWithItself() throws Exception {
    User ana = newUser();
    long tripId = createTrip(ana, "");
    long stopId = addStop(ana, tripId, lisbon());

    send(
            ana,
            put("/api/v1/trips/{t}/stops/{s}", tripId, stopId),
            """
            {"version": 0, "cityName": "Lisboa", "countryCode": "PT", "timezone": "Europe/Lisbon",
             "arrivalDate": "2027-03-11", "departureDate": "2027-03-15"}
            """)
        .andExpect(status().isOk());
  }

  @Test
  void validation() throws Exception {
    User ana = newUser();
    long tripId = createTrip(ana, "");

    send(
            ana,
            post("/api/v1/trips/{t}/stops", tripId),
            stop("Lisboa", "PT", "Europe/Lisbon", "2027-03-14", "2027-03-10"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("departureDate"));
    send(
            ana,
            post("/api/v1/trips/{t}/stops", tripId),
            stop("Lisboa", "pt", "Lua/Crateras", "2027-03-10", "2027-03-14"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.length()").value(2));
    send(
            ana,
            post("/api/v1/trips/{t}/stops", tripId),
            "{\"cityName\": \"\", \"countryCode\": \"PT\", \"timezone\": \"Europe/Lisbon\"}")
        .andExpect(status().isBadRequest());
  }

  @Test
  void reorder() throws Exception {
    User ana = newUser();
    long tripId = createTrip(ana, "");
    long lisbonId = addStop(ana, tripId, lisbon());
    long portoId = addStop(ana, tripId, porto());

    send(
            ana,
            put("/api/v1/trips/{t}/stops/order", tripId),
            "{\"stopIds\": [%d, %d]}".formatted(portoId, lisbonId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(portoId))
        .andExpect(jsonPath("$[0].seq").value(1))
        .andExpect(jsonPath("$[1].id").value(lisbonId))
        .andExpect(jsonPath("$[1].seq").value(2));
    mvc.perform(get("/api/v1/trips/{t}/stops", tripId).with(ana.session()))
        .andExpect(jsonPath("$[0].cityName").value("Porto"));

    send(
            ana,
            put("/api/v1/trips/{t}/stops/order", tripId),
            "{\"stopIds\": [%d]}".formatted(portoId))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("stopIds"));
    send(
            ana,
            put("/api/v1/trips/{t}/stops/order", tripId),
            "{\"stopIds\": [%d, %d]}".formatted(portoId, portoId))
        .andExpect(status().isBadRequest());
  }

  @Test
  void accessByRole() throws Exception {
    User owner = newUser();
    User viewer = newUser();
    User editor = newUser();
    User stranger = newUser();
    long tripId = createTrip(owner, "");
    long stopId = addStop(owner, tripId, lisbon());
    addMember(tripId, viewer, TripRole.VIEWER);
    addMember(tripId, editor, TripRole.EDITOR);

    mvc.perform(get("/api/v1/trips/{t}/stops", tripId).with(viewer.session()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1));
    send(viewer, post("/api/v1/trips/{t}/stops", tripId), porto())
        .andExpect(status().isForbidden());
    mvc.perform(
            delete("/api/v1/trips/{t}/stops/{s}", tripId, stopId)
                .with(viewer.session())
                .with(csrf()))
        .andExpect(status().isForbidden());

    addStop(editor, tripId, porto());

    mvc.perform(get("/api/v1/trips/{t}/stops", tripId).with(stranger.session()))
        .andExpect(status().isNotFound());
    send(stranger, post("/api/v1/trips/{t}/stops", tripId), porto())
        .andExpect(status().isNotFound());
  }

  @Test
  void stopOfAnotherTripIs404() throws Exception {
    User ana = newUser();
    long tripA = createTrip(ana, "");
    long tripB = createTrip(ana, "");
    long stopA = addStop(ana, tripA, lisbon());

    mvc.perform(
            delete("/api/v1/trips/{t}/stops/{s}", tripB, stopA).with(ana.session()).with(csrf()))
        .andExpect(status().isNotFound());
  }

  @Test
  void deletingTheTripRemovesStops() throws Exception {
    User ana = newUser();
    long tripId = createTrip(ana, "");
    addStop(ana, tripId, lisbon());

    mvc.perform(delete("/api/v1/trips/{t}", tripId).with(ana.session()).with(csrf()))
        .andExpect(status().isNoContent());
    mvc.perform(get("/api/v1/trips/{t}/stops", tripId).with(ana.session()))
        .andExpect(status().isNotFound());
  }
}
