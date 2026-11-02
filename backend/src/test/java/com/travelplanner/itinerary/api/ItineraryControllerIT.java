package com.travelplanner.itinerary.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.travelplanner.IntegrationTest;
import com.travelplanner.auth.TestUsers;
import com.travelplanner.auth.domain.AppUser;
import com.travelplanner.auth.domain.AppUserService;
import com.travelplanner.trip.domain.TripMember;
import com.travelplanner.trip.domain.TripRole;
import com.travelplanner.trip.infra.TripMemberRepository;
import com.travelplanner.trip.infra.TripRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

class ItineraryControllerIT extends IntegrationTest {

  @Autowired MockMvc mvc;
  @Autowired AppUserService users;
  @Autowired TripRepository trips;
  @Autowired TripMemberRepository members;

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

  private long create(User user, String path, String json, Object... vars) throws Exception {
    return id(send(user, post(path, vars), json).andExpect(status().isCreated()));
  }

  private long stop(User user, long tripId, String city, String arrival, String departure)
      throws Exception {
    return create(
        user,
        "/api/v1/trips/{t}/stops",
        """
        {"cityName": "%s", "countryCode": "PT", "timezone": "Europe/Lisbon",
         "arrivalDate": "%s", "departureDate": "%s"}
        """
            .formatted(city, arrival, departure),
        tripId);
  }

  private void lodging(User user, long tripId, long stopId, String name, String in, String out)
      throws Exception {
    create(
        user,
        "/api/v1/trips/{t}/stops/{s}/lodgings",
        """
        {"kind": "HOTEL", "name": "%s", "checkIn": "%s", "checkOut": "%s"}
        """
            .formatted(name, in, out),
        tripId,
        stopId);
  }

  @Test
  void dayByDayFromTheItinerary() throws Exception {
    User ana = newUser();
    long tripId =
        create(ana, "/api/v1/trips", "{\"name\": \"Portugal\", \"status\": \"PLANNING\"}");
    long lisbon = stop(ana, tripId, "Lisboa", "2027-03-11", "2027-03-14");
    long porto = stop(ana, tripId, "Porto", "2027-03-14", "2027-03-17");
    create(
        ana,
        "/api/v1/trips/{t}/segments",
        """
        {"type": "FLIGHT", "destinationStopId": %d, "originLabel": "GRU",
         "departAt": "2027-03-10T22:40", "departTz": "America/Sao_Paulo",
         "arriveAt": "2027-03-11T11:15", "arriveTz": "Europe/Lisbon", "code": "TP 88"}
        """
            .formatted(lisbon),
        tripId);
    create(
        ana,
        "/api/v1/trips/{t}/segments",
        """
        {"type": "TRAIN", "originStopId": %d, "destinationStopId": %d,
         "departAt": "2027-03-14T10:00", "departTz": "Europe/Lisbon"}
        """
            .formatted(lisbon, porto),
        tripId);
    lodging(ana, tripId, lisbon, "Hotel Avenida", "2027-03-11", "2027-03-14");
    lodging(ana, tripId, porto, "Porto Ribeira", "2027-03-14", "2027-03-16");

    mvc.perform(get("/api/v1/trips/{t}/itinerary", tripId).with(ana.session()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.days.length()").value(8))
        // 10/03: voo noturno saindo de casa
        .andExpect(jsonPath("$.days[0].date").value("2027-03-10"))
        .andExpect(jsonPath("$.days[0].stops.length()").value(0))
        .andExpect(jsonPath("$.days[0].events[0].kind").value("SEGMENT_DEPARTURE"))
        .andExpect(jsonPath("$.days[0].events[0].time").value("22:40"))
        .andExpect(jsonPath("$.days[0].events[0].instant").value("2027-03-10T22:40:00-03:00"))
        .andExpect(jsonPath("$.days[0].events[0].segment.origin").value("GRU"))
        .andExpect(jsonPath("$.days[0].events[0].segment.destination").value("Lisboa"))
        .andExpect(jsonPath("$.days[0].events[0].segment.code").value("TP 88"))
        .andExpect(jsonPath("$.days[0].overnight.kind").value("IN_TRANSIT"))
        .andExpect(jsonPath("$.days[0].gap").value(false))
        // 11/03: chegada e check-in
        .andExpect(jsonPath("$.days[1].stops[0].cityName").value("Lisboa"))
        .andExpect(jsonPath("$.days[1].events[0].kind").value("SEGMENT_ARRIVAL"))
        .andExpect(jsonPath("$.days[1].events[0].time").value("11:15"))
        .andExpect(jsonPath("$.days[1].events[1].kind").value("CHECK_IN"))
        .andExpect(jsonPath("$.days[1].events[1].time").doesNotExist())
        .andExpect(jsonPath("$.days[1].overnight.kind").value("LODGING"))
        .andExpect(jsonPath("$.days[1].overnight.lodging.name").value("Hotel Avenida"))
        // 14/03: troca de cidade
        .andExpect(jsonPath("$.days[4].stops.length()").value(2))
        .andExpect(jsonPath("$.days[4].events.length()").value(3))
        .andExpect(jsonPath("$.days[4].events[1].segment.origin").value("Lisboa"))
        .andExpect(jsonPath("$.days[4].events[1].segment.destination").value("Porto"))
        // 16/03: noite descoberta no Porto
        .andExpect(jsonPath("$.days[6].overnight.kind").value("MISSING"))
        .andExpect(jsonPath("$.days[6].gap").value(true))
        .andExpect(jsonPath("$.days[7].overnight.kind").value("HOME"));
  }

  @Test
  void tripWithoutDatesHasNoDays() throws Exception {
    User ana = newUser();
    long tripId =
        create(ana, "/api/v1/trips", "{\"name\": \"Algum dia\", \"status\": \"PLANNING\"}");

    mvc.perform(get("/api/v1/trips/{t}/itinerary", tripId).with(ana.session()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.days.length()").value(0));
  }

  @Test
  void viewerReadsAndStrangerGets404() throws Exception {
    User ana = newUser();
    User bia = newUser();
    User caio = newUser();
    long tripId =
        create(ana, "/api/v1/trips", "{\"name\": \"Portugal\", \"status\": \"PLANNING\"}");
    stop(ana, tripId, "Lisboa", "2027-03-11", "2027-03-14");
    members.save(
        TripMember.of(
            trips.findById(tripId).orElseThrow(), bia.id(), TripRole.VIEWER, Instant.now()));

    mvc.perform(get("/api/v1/trips/{t}/itinerary", tripId).with(bia.session()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.days.length()").value(4))
        .andExpect(jsonPath("$.days[0].gap").value(true));
    mvc.perform(get("/api/v1/trips/{t}/itinerary", tripId).with(caio.session()))
        .andExpect(status().isNotFound());
    mvc.perform(get("/api/v1/trips/{t}/itinerary", tripId)).andExpect(status().isUnauthorized());
  }
}
