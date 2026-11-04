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
class LodgingControllerIT extends IntegrationTest {

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

  private long createTrip(User user) throws Exception {
    return id(
        send(user, post("/api/v1/trips"), "{\"name\": \"Portugal\", \"status\": \"PLANNING\"}")
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

  private static String lodging(String name, String checkIn, String checkOut, String extra) {
    return """
        {"kind": "HOTEL", "name": "%s", "checkIn": "%s", "checkOut": "%s" %s}
        """
        .formatted(name, checkIn, checkOut, extra);
  }

  private long addLodging(User user, long tripId, long stopId, String json) throws Exception {
    return id(
        send(user, post("/api/v1/trips/{t}/stops/{s}/lodgings", tripId, stopId), json)
            .andExpect(status().isCreated()));
  }

  private void addMember(long tripId, User user, TripRole role) {
    members.save(
        TripMember.of(trips.findById(tripId).orElseThrow(), user.id(), role, Instant.now()));
  }

  @Test
  void twoHotelsInTheSameCity() throws Exception {
    User ana = newUser();
    long tripId = createTrip(ana);
    long lisbon = addStop(ana, tripId, "Lisboa", "2027-03-11", "2027-03-14");
    long porto = addStop(ana, tripId, "Porto", "2027-03-14", "2027-03-17");

    send(
            ana,
            post("/api/v1/trips/{t}/stops/{s}/lodgings", tripId, lisbon),
            lodging(
                " Hotel Avenida ",
                "2027-03-11",
                "2027-03-13",
                """
                , "address": "Av. da Liberdade, 1", "boardType": "BREAKFAST", "priceTier": 3,
                  "bookingRef": "BK-1", "notes": " "
                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.stopId").value(lisbon))
        .andExpect(jsonPath("$.kind").value("HOTEL"))
        .andExpect(jsonPath("$.name").value("Hotel Avenida"))
        .andExpect(jsonPath("$.nights").value(2))
        .andExpect(jsonPath("$.boardType").value("BREAKFAST"))
        .andExpect(jsonPath("$.priceTier").value(3))
        .andExpect(jsonPath("$.priceTierSource").value("USER"))
        .andExpect(jsonPath("$.placeId").doesNotExist())
        .andExpect(jsonPath("$.notes").doesNotExist())
        .andExpect(jsonPath("$.version").value(0));
    addLodging(ana, tripId, lisbon, lodging("Casa Alfama", "2027-03-13", "2027-03-14", ""));
    addLodging(ana, tripId, porto, lodging("Porto Ribeira", "2027-03-14", "2027-03-17", ""));

    mvc.perform(get("/api/v1/trips/{t}/stops/{s}/lodgings", tripId, lisbon).with(ana.session()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].name").value("Hotel Avenida"))
        .andExpect(jsonPath("$[1].name").value("Casa Alfama"))
        .andExpect(jsonPath("$[1].priceTier").doesNotExist())
        .andExpect(jsonPath("$[1].priceTierSource").doesNotExist());
    mvc.perform(get("/api/v1/trips/{t}/lodgings", tripId).with(ana.session()))
        .andExpect(jsonPath("$.length()").value(3))
        .andExpect(jsonPath("$[2].stopId").value(porto));

    assertThat(events.stream(TripChangedEvent.class)).contains(new TripChangedEvent(tripId));
  }

  @Test
  void friendsHomeDropsBoardPriceAndBooking() throws Exception {
    User ana = newUser();
    long tripId = createTrip(ana);
    long porto = addStop(ana, tripId, "Porto", "2027-03-14", "2027-03-17");

    send(
            ana,
            post("/api/v1/trips/{t}/stops/{s}/lodgings", tripId, porto),
            """
            {"kind": "FRIENDS_HOME", "name": "Casa do primo João", "checkIn": "2027-03-14",
             "checkOut": "2027-03-17", "boardType": "BREAKFAST", "priceTier": 2,
             "bookingRef": "X", "address": "Rua das Flores, 10"}
            """)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.kind").value("FRIENDS_HOME"))
        .andExpect(jsonPath("$.address").value("Rua das Flores, 10"))
        .andExpect(jsonPath("$.boardType").doesNotExist())
        .andExpect(jsonPath("$.priceTier").doesNotExist())
        .andExpect(jsonPath("$.priceTierSource").doesNotExist())
        .andExpect(jsonPath("$.bookingRef").doesNotExist());
    // Sem tipo → 400.
    send(
            ana,
            post("/api/v1/trips/{t}/stops/{s}/lodgings", tripId, porto),
            "{\"name\": \"Casa\", \"checkIn\": \"2027-03-14\", \"checkOut\": \"2027-03-15\"}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("kind"));
  }

  @Test
  void updateDeleteAndConflict() throws Exception {
    User ana = newUser();
    long tripId = createTrip(ana);
    long lisbon = addStop(ana, tripId, "Lisboa", "2027-03-11", "2027-03-14");
    long lodgingId =
        addLodging(
            ana,
            tripId,
            lisbon,
            lodging("Hotel Avenida", "2027-03-11", "2027-03-14", ", \"priceTier\": 2"));
    String path = "/api/v1/trips/{t}/stops/{s}/lodgings/{l}";

    send(
            ana,
            put(path, tripId, lisbon, lodgingId),
            lodging(
                "Hotel Avenida Palace",
                "2027-03-12",
                "2027-03-14",
                ", \"version\": 0, \"boardType\": \"ALL_INCLUSIVE\""))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.version").value(1))
        .andExpect(jsonPath("$.name").value("Hotel Avenida Palace"))
        .andExpect(jsonPath("$.boardType").value("ALL_INCLUSIVE"))
        .andExpect(jsonPath("$.priceTier").doesNotExist())
        .andExpect(jsonPath("$.priceTierSource").doesNotExist());
    send(
            ana,
            put(path, tripId, lisbon, lodgingId),
            lodging("Hotel", "2027-03-12", "2027-03-14", ", \"version\": 0"))
        .andExpect(status().isConflict());

    mvc.perform(delete(path, tripId, lisbon, lodgingId).with(ana.session()).with(csrf()))
        .andExpect(status().isNoContent());
    mvc.perform(get("/api/v1/trips/{t}/lodgings", tripId).with(ana.session()))
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void validation() throws Exception {
    User ana = newUser();
    long tripId = createTrip(ana);
    long lisbon = addStop(ana, tripId, "Lisboa", "2027-03-11", "2027-03-14");
    long otherTrip = createTrip(ana);
    long faro = addStop(ana, otherTrip, "Faro", "2027-05-01", "2027-05-03");
    String path = "/api/v1/trips/{t}/stops/{s}/lodgings";

    send(ana, post(path, tripId, lisbon), lodging("Hotel", "2027-03-12", "2027-03-12", ""))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("checkOut"));
    send(ana, post(path, tripId, lisbon), lodging("Hotel", "2027-03-10", "2027-03-12", ""))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("checkIn"));
    send(ana, post(path, tripId, lisbon), lodging("Hotel", "2027-03-12", "2027-03-15", ""))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("checkOut"));
    // Faixa de preço fora de 1–4; nome em branco (sem placeId, o nome é obrigatório).
    send(
            ana,
            post(path, tripId, lisbon),
            lodging("Hotel", "2027-03-11", "2027-03-12", ", \"priceTier\": 5"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("priceTier"));
    send(ana, post(path, tripId, lisbon), lodging(" ", "2027-03-11", "2027-03-12", ""))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("name"));
    // Parada de outra viagem.
    send(ana, post(path, tripId, faro), lodging("Hotel", "2027-05-01", "2027-05-02", ""))
        .andExpect(status().isNotFound());
  }

  @Test
  void stopDatesMustKeepItsLodgingsAndDeletingTheStopRemovesThem() throws Exception {
    User ana = newUser();
    long tripId = createTrip(ana);
    long lisbon = addStop(ana, tripId, "Lisboa", "2027-03-11", "2027-03-14");
    addLodging(ana, tripId, lisbon, lodging("Hotel Avenida", "2027-03-11", "2027-03-14", ""));

    String shorter =
        """
        {"version": 0, "cityName": "Lisboa", "countryCode": "PT", "timezone": "Europe/Lisbon",
         "arrivalDate": "2027-03-11", "departureDate": "2027-03-13"}
        """;
    send(ana, put("/api/v1/trips/{t}/stops/{s}", tripId, lisbon), shorter)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("arrivalDate"));
    String longer = shorter.replace("2027-03-13", "2027-03-15");
    send(ana, put("/api/v1/trips/{t}/stops/{s}", tripId, lisbon), longer)
        .andExpect(status().isOk());

    mvc.perform(
            delete("/api/v1/trips/{t}/stops/{s}", tripId, lisbon).with(ana.session()).with(csrf()))
        .andExpect(status().isNoContent());
    mvc.perform(get("/api/v1/trips/{t}/lodgings", tripId).with(ana.session()))
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void accessByRole() throws Exception {
    User owner = newUser();
    User viewer = newUser();
    User editor = newUser();
    User stranger = newUser();
    long tripId = createTrip(owner);
    long lisbon = addStop(owner, tripId, "Lisboa", "2027-03-11", "2027-03-14");
    String hotel = lodging("Hotel", "2027-03-11", "2027-03-14", "");
    long lodgingId = addLodging(owner, tripId, lisbon, hotel);
    addMember(tripId, viewer, TripRole.VIEWER);
    addMember(tripId, editor, TripRole.EDITOR);

    mvc.perform(get("/api/v1/trips/{t}/lodgings", tripId).with(viewer.session()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1));
    send(viewer, post("/api/v1/trips/{t}/stops/{s}/lodgings", tripId, lisbon), hotel)
        .andExpect(status().isForbidden());
    mvc.perform(
            delete("/api/v1/trips/{t}/stops/{s}/lodgings/{l}", tripId, lisbon, lodgingId)
                .with(viewer.session())
                .with(csrf()))
        .andExpect(status().isForbidden());

    addLodging(editor, tripId, lisbon, hotel);

    mvc.perform(get("/api/v1/trips/{t}/lodgings", tripId).with(stranger.session()))
        .andExpect(status().isNotFound());
    mvc.perform(
            get("/api/v1/trips/{t}/stops/{s}/lodgings", tripId, lisbon).with(stranger.session()))
        .andExpect(status().isNotFound());
  }
}
