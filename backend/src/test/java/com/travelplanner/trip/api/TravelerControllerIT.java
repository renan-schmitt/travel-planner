package com.travelplanner.trip.api;

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
import com.travelplanner.trip.infra.TravelerRepository;
import com.travelplanner.trip.infra.TripMemberRepository;
import com.travelplanner.trip.infra.TripRepository;
import java.time.Instant;
import java.util.Arrays;
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
class TravelerControllerIT extends IntegrationTest {

  @Autowired MockMvc mvc;
  @Autowired AppUserService users;
  @Autowired TravelerRepository travelers;
  @Autowired TripRepository trips;
  @Autowired TripMemberRepository members;
  @Autowired ApplicationEvents events;

  private record User(long id, RequestPostProcessor session) {}

  private User newUser(String name) {
    var google = TestUsers.googleUser(UUID.randomUUID() + "@example.com", name);
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

  private long id(ResultActions result) throws Exception {
    String body = result.andReturn().getResponse().getContentAsString();
    return ((Number) JsonPath.read(body, "$.id")).longValue();
  }

  private long createTraveler(User user, String name) throws Exception {
    return id(
        send(
                user,
                post("/api/v1/travelers"),
                """
                {"name": "%s", "ageGroup": "CHILD", "nationalities": ["BR"]}
                """
                    .formatted(name))
            .andExpect(status().isCreated()));
  }

  private long createTrip(User user) throws Exception {
    return id(
        send(user, post("/api/v1/trips"), "{\"name\": \"Lisboa\", \"status\": \"PLANNING\"}")
            .andExpect(status().isCreated()));
  }

  private String ids(long... ids) {
    return "{\"travelerIds\": %s}".formatted(Arrays.toString(ids));
  }

  @Test
  void firstLoginCreatesSelfTraveler() throws Exception {
    User ana = newUser("Ana Souza");

    mvc.perform(get("/api/v1/travelers").with(ana.session()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].name").value("Ana Souza"))
        .andExpect(jsonPath("$[0].ageGroup").value("ADULT"))
        .andExpect(jsonPath("$[0].nationalities[0]").value("BR"))
        .andExpect(jsonPath("$[0].me").value(true))
        .andExpect(jsonPath("$[0].mine").value(true));

    // newUser() faz dois logins (upsert + sessão): continua havendo só um "eu".
    assertThat(travelers.findByOwnerUserId(ana.id())).hasSize(1);
  }

  @Test
  void crudOfOwnTravelers() throws Exception {
    User ana = newUser("Ana");

    long leo =
        id(
            send(
                    ana,
                    post("/api/v1/travelers"),
                    """
                    {"name": " Léo ", "ageGroup": "CHILD", "birthYear": 2018,
                     "nationalities": ["BR", "PT", "BR"], "passportExpiry": "2030-05-01"}
                    """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Léo"))
                .andExpect(jsonPath("$.birthYear").value(2018))
                .andExpect(jsonPath("$.nationalities.length()").value(2))
                .andExpect(jsonPath("$.nationalities[1]").value("PT"))
                .andExpect(jsonPath("$.passportExpiry").value("2030-05-01"))
                .andExpect(jsonPath("$.me").value(false))
                .andExpect(jsonPath("$.version").value(0)));

    mvc.perform(get("/api/v1/travelers").with(ana.session()))
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].me").value(true))
        .andExpect(jsonPath("$[1].name").value("Léo"));

    send(
            ana,
            put("/api/v1/travelers/" + leo),
            """
            {"version": 0, "name": "Leonardo", "ageGroup": "TEEN", "nationalities": ["BR"]}
            """)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Leonardo"))
        .andExpect(jsonPath("$.ageGroup").value("TEEN"))
        .andExpect(jsonPath("$.passportExpiry").doesNotExist())
        .andExpect(jsonPath("$.version").value(1));

    // Versão antiga → 409.
    send(
            ana,
            put("/api/v1/travelers/" + leo),
            """
            {"version": 0, "name": "Leo", "ageGroup": "TEEN", "nationalities": ["BR"]}
            """)
        .andExpect(status().isConflict());

    mvc.perform(delete("/api/v1/travelers/" + leo).with(ana.session()).with(csrf()))
        .andExpect(status().isNoContent());
    assertThat(travelers.findById(leo)).isEmpty();
  }

  @Test
  void validatesInput() throws Exception {
    User ana = newUser("Ana");

    send(
            ana,
            post("/api/v1/travelers"),
            """
            {"name": "", "ageGroup": "ADULT", "nationalities": ["br"], "birthYear": 1800}
            """)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.length()").value(3));
    send(ana, post("/api/v1/travelers"), "{\"name\": \"X\", \"ageGroup\": \"ADULT\"}")
        .andExpect(status().isBadRequest());
  }

  @Test
  void selfTravelerCannotBeDeleted() throws Exception {
    User ana = newUser("Ana");
    long self = travelers.findSelf(ana.id()).orElseThrow().getId();

    mvc.perform(delete("/api/v1/travelers/" + self).with(ana.session()).with(csrf()))
        .andExpect(status().isConflict());
  }

  @Test
  void otherUsersTravelersAreInvisible() throws Exception {
    User ana = newUser("Ana");
    User bia = newUser("Bia");
    long leo = createTraveler(ana, "Léo");

    mvc.perform(get("/api/v1/travelers").with(bia.session()))
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].name").value("Bia"));
    send(
            bia,
            put("/api/v1/travelers/" + leo),
            """
            {"version": 0, "name": "Hack", "ageGroup": "ADULT", "nationalities": ["BR"]}
            """)
        .andExpect(status().isNotFound());
    mvc.perform(delete("/api/v1/travelers/" + leo).with(bia.session()).with(csrf()))
        .andExpect(status().isNotFound());
  }

  @Test
  void newTripIncludesCreatorsSelfTraveler() throws Exception {
    User ana = newUser("Ana");
    long tripId = createTrip(ana);

    mvc.perform(get("/api/v1/trips/" + tripId + "/travelers").with(ana.session()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].name").value("Ana"))
        .andExpect(jsonPath("$[0].me").value(true));
  }

  @Test
  void familyIsReusedAcrossTrips() throws Exception {
    User ana = newUser("Ana");
    long self = travelers.findSelf(ana.id()).orElseThrow().getId();
    long leo = createTraveler(ana, "Léo");
    long first = createTrip(ana);
    long second = createTrip(ana);
    events.clear();

    send(ana, put("/api/v1/trips/" + first + "/travelers"), ids(self, leo))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2));
    send(ana, put("/api/v1/trips/" + second + "/travelers"), ids(leo))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("Léo"));

    mvc.perform(get("/api/v1/trips/" + first + "/travelers").with(ana.session()))
        .andExpect(jsonPath("$.length()").value(2));
    mvc.perform(get("/api/v1/trips/" + second + "/travelers").with(ana.session()))
        .andExpect(jsonPath("$.length()").value(1));
    assertThat(events.stream(TripChangedEvent.class).map(TripChangedEvent::tripId))
        .containsExactly(first, second);

    // Editar o viajante avisa as duas viagens; excluir tira ele delas.
    events.clear();
    send(
            ana,
            put("/api/v1/travelers/" + leo),
            """
            {"version": 0, "name": "Léo", "ageGroup": "CHILD", "nationalities": ["BR"],
             "passportExpiry": "2027-01-10"}
            """)
        .andExpect(status().isOk());
    assertThat(events.stream(TripChangedEvent.class).map(TripChangedEvent::tripId))
        .containsExactlyInAnyOrder(first, second);

    mvc.perform(delete("/api/v1/travelers/" + leo).with(ana.session()).with(csrf()))
        .andExpect(status().isNoContent());
    mvc.perform(get("/api/v1/trips/" + first + "/travelers").with(ana.session()))
        .andExpect(jsonPath("$.length()").value(1));
  }

  @Test
  void cannotAddAnotherUsersTraveler() throws Exception {
    User ana = newUser("Ana");
    User bia = newUser("Bia");
    long biaSelf = travelers.findSelf(bia.id()).orElseThrow().getId();
    long tripId = createTrip(ana);

    send(ana, put("/api/v1/trips/" + tripId + "/travelers"), ids(biaSelf))
        .andExpect(status().isNotFound());
    send(ana, put("/api/v1/trips/" + tripId + "/travelers"), ids(999_999_999L))
        .andExpect(status().isNotFound());
  }

  @Test
  void editorBringsOwnTravelersAndKeepsOthers() throws Exception {
    User ana = newUser("Ana");
    User bia = newUser("Bia");
    long anaSelf = travelers.findSelf(ana.id()).orElseThrow().getId();
    long biaSelf = travelers.findSelf(bia.id()).orElseThrow().getId();
    long tripId = createTrip(ana);
    members.save(
        TripMember.of(
            trips.findById(tripId).orElseThrow(), bia.id(), TripRole.EDITOR, Instant.now()));

    // Bia mantém a Ana (já está na viagem) e acrescenta a si mesma.
    send(bia, put("/api/v1/trips/" + tripId + "/travelers"), ids(anaSelf, biaSelf))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("Bia"))
        .andExpect(jsonPath("$[0].me").value(true))
        .andExpect(jsonPath("$[1].name").value("Ana"))
        .andExpect(jsonPath("$[1].mine").value(false));

    // A Ana vê a Bia na viagem, mas não na lista dela.
    mvc.perform(get("/api/v1/trips/" + tripId + "/travelers").with(ana.session()))
        .andExpect(jsonPath("$[0].name").value("Ana"))
        .andExpect(jsonPath("$[1].name").value("Bia"))
        .andExpect(jsonPath("$[1].mine").value(false));
    mvc.perform(get("/api/v1/travelers").with(ana.session()))
        .andExpect(jsonPath("$.length()").value(1));
  }

  @Test
  void tripTravelersRequireMembershipAndRole() throws Exception {
    User ana = newUser("Ana");
    User bia = newUser("Bia");
    User caio = newUser("Caio");
    long tripId = createTrip(ana);
    members.save(
        TripMember.of(
            trips.findById(tripId).orElseThrow(), bia.id(), TripRole.VIEWER, Instant.now()));

    mvc.perform(get("/api/v1/trips/" + tripId + "/travelers").with(caio.session()))
        .andExpect(status().isNotFound());
    mvc.perform(get("/api/v1/trips/" + tripId + "/travelers").with(bia.session()))
        .andExpect(status().isOk());
    send(bia, put("/api/v1/trips/" + tripId + "/travelers"), ids())
        .andExpect(status().isForbidden());
    send(caio, put("/api/v1/trips/" + tripId + "/travelers"), ids())
        .andExpect(status().isNotFound());
  }
}
