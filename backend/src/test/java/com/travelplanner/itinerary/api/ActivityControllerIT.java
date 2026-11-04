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
import com.travelplanner.itinerary.infra.ActivityRepository;
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

/** Atividades da timeline: CRUD, reordenação/troca de dia, validação e a timeline com elas. */
class ActivityControllerIT extends IntegrationTest {

  @Autowired MockMvc mvc;
  @Autowired AppUserService users;
  @Autowired TripRepository trips;
  @Autowired TripMemberRepository members;
  @Autowired ActivityRepository activities;

  private record User(long id, RequestPostProcessor session) {}

  /** Viagem de 10 a 17/03 com Lisboa (11–14) e um restaurante favorito. */
  private record Trip(long id, long stopId, long restaurantId) {}

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

  private ResultActions fetch(User user, String url, Object... vars) throws Exception {
    return mvc.perform(get(url, vars).with(user.session()));
  }

  private static long id(ResultActions result) throws Exception {
    String body = result.andReturn().getResponse().getContentAsString();
    return ((Number) JsonPath.read(body, "$.id")).longValue();
  }

  private Trip createTrip(User user) throws Exception {
    long tripId =
        id(
            send(
                    user,
                    post("/api/v1/trips"),
                    """
                    {"name": "Portugal", "status": "PLANNING",
                     "startDate": "2027-03-10", "endDate": "2027-03-17"}
                    """)
                .andExpect(status().isCreated()));
    long stopId =
        id(
            send(
                    user,
                    post("/api/v1/trips/{t}/stops", tripId),
                    """
                    {"cityName": "Lisboa", "countryCode": "PT", "timezone": "Europe/Lisbon",
                     "arrivalDate": "2027-03-11", "departureDate": "2027-03-14"}
                    """)
                .andExpect(status().isCreated()));
    long restaurantId =
        id(
            send(
                    user,
                    post("/api/v1/trips/{t}/stops/{s}/saved-places", tripId, stopId),
                    """
                    {"placeId": "ChIJBelcanto", "kind": "RESTAURANT", "name": "Belcanto"}
                    """)
                .andExpect(status().isCreated()));
    return new Trip(tripId, stopId, restaurantId);
  }

  private long addActivity(User user, long tripId, String json) throws Exception {
    return id(
        send(user, post("/api/v1/trips/{t}/activities", tripId), json)
            .andExpect(status().isCreated()));
  }

  private void addMember(long tripId, User user, TripRole role) {
    members.save(
        TripMember.of(trips.findById(tripId).orElseThrow(), user.id(), role, Instant.now()));
  }

  @Test
  void planADayWithTimedFreeAndFavoriteActivities() throws Exception {
    User ana = newUser();
    Trip trip = createTrip(ana);

    long museum =
        addActivity(ana, trip.id(), "{\"day\": \"2027-03-12\", \"title\": \" Museu do Azulejo \"}");
    send(
            ana,
            post("/api/v1/trips/{t}/activities", trip.id()),
            """
            {"day": "2027-03-12", "savedPlaceId": %d, "startTime": "20:00", "notes": "Reserva 4p"}
            """
                .formatted(trip.restaurantId()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.title").value("Belcanto"))
        .andExpect(jsonPath("$.savedPlaceId").value(trip.restaurantId()))
        .andExpect(jsonPath("$.startTime").value("20:00"))
        .andExpect(jsonPath("$.endTime").doesNotExist())
        .andExpect(jsonPath("$.version").value(0));
    send(
            ana,
            post("/api/v1/trips/{t}/activities", trip.id()),
            """
            {"day": "2027-03-12", "title": "Castelo de São Jorge",
             "startTime": "09:30", "endTime": "11:00"}
            """)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.endTime").value("11:00"));
    long tram =
        addActivity(ana, trip.id(), "{\"day\": \"2027-03-12\", \"title\": \"Elétrico 28\"}");

    fetch(ana, "/api/v1/trips/{t}/activities", trip.id())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(4))
        .andExpect(jsonPath("$[0].id").value(museum))
        .andExpect(jsonPath("$[0].sortOrder").value(0))
        .andExpect(jsonPath("$[3].id").value(tram))
        .andExpect(jsonPath("$[3].sortOrder").value(3));

    // Na timeline: com horário pelo horário; sem horário depois, na ordem do usuário.
    fetch(ana, "/api/v1/trips/{t}/itinerary", trip.id())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.outsideActivities.length()").value(0))
        .andExpect(jsonPath("$.days[2].date").value("2027-03-12"))
        .andExpect(jsonPath("$.days[2].events.length()").value(4))
        .andExpect(jsonPath("$.days[2].events[0].kind").value("ACTIVITY"))
        .andExpect(jsonPath("$.days[2].events[0].time").value("09:30"))
        .andExpect(jsonPath("$.days[2].events[0].activity.title").value("Castelo de São Jorge"))
        .andExpect(jsonPath("$.days[2].events[0].activity.endTime").value("11:00"))
        .andExpect(jsonPath("$.days[2].events[1].activity.title").value("Belcanto"))
        .andExpect(jsonPath("$.days[2].events[1].activity.notes").value("Reserva 4p"))
        .andExpect(jsonPath("$.days[2].events[1].activity.savedPlace.name").value("Belcanto"))
        .andExpect(jsonPath("$.days[2].events[1].activity.savedPlace.kind").value("RESTAURANT"))
        .andExpect(jsonPath("$.days[2].events[1].activity.savedPlace.stopId").value(trip.stopId()))
        .andExpect(jsonPath("$.days[2].events[2].activity.title").value("Museu do Azulejo"))
        .andExpect(jsonPath("$.days[2].events[2].time").doesNotExist())
        .andExpect(jsonPath("$.days[2].events[3].activity.id").value(tram))
        .andExpect(jsonPath("$.days[2].events[3].activity.version").value(0));
  }

  @Test
  void reorderWithinADayAndMoveBetweenDays() throws Exception {
    User ana = newUser();
    Trip trip = createTrip(ana);
    long a = addActivity(ana, trip.id(), "{\"day\": \"2027-03-12\", \"title\": \"A\"}");
    long b = addActivity(ana, trip.id(), "{\"day\": \"2027-03-12\", \"title\": \"B\"}");
    long c = addActivity(ana, trip.id(), "{\"day\": \"2027-03-13\", \"title\": \"C\"}");
    String reorder = "/api/v1/trips/{t}/activities/reorder";

    send(
            ana,
            put(reorder, trip.id()),
            "{\"day\": \"2027-03-12\", \"activityIds\": [%d, %d]}".formatted(b, a))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(b))
        .andExpect(jsonPath("$[0].sortOrder").value(0))
        .andExpect(jsonPath("$[1].id").value(a));

    // C vem do dia 13 para o meio do dia 12.
    send(
            ana,
            put(reorder, trip.id()),
            "{\"day\": \"2027-03-12\", \"activityIds\": [%d, %d, %d]}".formatted(b, c, a))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[1].id").value(c))
        .andExpect(jsonPath("$[1].day").value("2027-03-12"))
        .andExpect(jsonPath("$[1].version").value(1));
    fetch(ana, "/api/v1/trips/{t}/itinerary", trip.id())
        .andExpect(jsonPath("$.days[2].events[0].activity.title").value("B"))
        .andExpect(jsonPath("$.days[2].events[1].activity.title").value("C"))
        .andExpect(jsonPath("$.days[2].events[2].activity.title").value("A"))
        .andExpect(jsonPath("$.days[3].events.length()").value(0));

    // Faltando uma atividade do dia, repetida, de outra viagem ou dia fora da viagem → 400.
    Trip other = createTrip(ana);
    long foreign = addActivity(ana, other.id(), "{\"day\": \"2027-03-12\", \"title\": \"X\"}");
    for (String ids :
        new String[] {
          "[%d, %d]".formatted(b, c),
          "[%d, %d, %d, %d]".formatted(b, c, a, a),
          "[%d, %d, %d, %d]".formatted(b, c, a, foreign)
        }) {
      send(
              ana,
              put(reorder, trip.id()),
              "{\"day\": \"2027-03-12\", \"activityIds\": %s}".formatted(ids))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.errors[0].field").value("activityIds"));
    }
    send(
            ana,
            put(reorder, trip.id()),
            "{\"day\": \"2027-03-18\", \"activityIds\": [%d]}".formatted(a))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("day"));
  }

  @Test
  void editConflictAndDelete() throws Exception {
    User ana = newUser();
    Trip trip = createTrip(ana);
    long first = addActivity(ana, trip.id(), "{\"day\": \"2027-03-13\", \"title\": \"Sintra\"}");
    long moved = addActivity(ana, trip.id(), "{\"day\": \"2027-03-12\", \"title\": \"Belém\"}");

    // Mudar o dia leva para o fim do novo dia.
    send(
            ana,
            put("/api/v1/trips/{t}/activities/{a}", trip.id(), moved),
            """
            {"version": 0, "day": "2027-03-13", "title": "Belém e Jerónimos",
             "startTime": "14:00", "savedPlaceId": %d}
            """
                .formatted(trip.restaurantId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Belém e Jerónimos"))
        .andExpect(jsonPath("$.day").value("2027-03-13"))
        .andExpect(jsonPath("$.sortOrder").value(1))
        .andExpect(jsonPath("$.version").value(1));
    send(
            ana,
            put("/api/v1/trips/{t}/activities/{a}", trip.id(), moved),
            "{\"version\": 0, \"day\": \"2027-03-13\", \"title\": \"Velha\"}")
        .andExpect(status().isConflict());

    // Excluir o favorito só desfaz o vínculo.
    mvc.perform(
            delete(
                    "/api/v1/trips/{t}/stops/{s}/saved-places/{p}",
                    trip.id(),
                    trip.stopId(),
                    trip.restaurantId())
                .with(ana.session())
                .with(csrf()))
        .andExpect(status().isNoContent());
    fetch(ana, "/api/v1/trips/{t}/activities", trip.id())
        .andExpect(jsonPath("$[1].id").value(moved))
        .andExpect(jsonPath("$[1].savedPlaceId").doesNotExist());

    mvc.perform(
            delete("/api/v1/trips/{t}/activities/{a}", trip.id(), first)
                .with(ana.session())
                .with(csrf()))
        .andExpect(status().isNoContent());
    mvc.perform(
            delete("/api/v1/trips/{t}/activities/{a}", trip.id(), first)
                .with(ana.session())
                .with(csrf()))
        .andExpect(status().isNotFound());

    // Excluir a viagem exclui as atividades.
    mvc.perform(delete("/api/v1/trips/{t}", trip.id()).with(ana.session()).with(csrf()))
        .andExpect(status().isNoContent());
    assertThat(activities.findById(moved)).isEmpty();
  }

  @Test
  void validation() throws Exception {
    User ana = newUser();
    Trip trip = createTrip(ana);
    Trip other = createTrip(ana);
    String url = "/api/v1/trips/{t}/activities";

    String[][] cases = {
      {"{\"day\": \"2027-03-09\", \"title\": \"Antes\"}", "day"},
      {"{\"day\": \"2027-03-18\", \"title\": \"Depois\"}", "day"},
      {"{\"title\": \"Sem dia\"}", "day"},
      {"{\"day\": \"2027-03-12\"}", "title"},
      {"{\"day\": \"2027-03-12\", \"title\": \"   \"}", "title"},
      {"{\"day\": \"2027-03-12\", \"title\": \"X\", \"endTime\": \"10:00\"}", "startTime"},
      {
        "{\"day\": \"2027-03-12\", \"title\": \"X\", \"startTime\": \"10:00\", \"endTime\":"
            + " \"10:00\"}",
        "endTime"
      },
      {"{\"day\": \"2027-03-12\", \"title\": \"X\", \"startTime\": \"25:00\"}", "startTime"},
      {
        "{\"day\": \"2027-03-12\", \"savedPlaceId\": %d}".formatted(other.restaurantId()),
        "savedPlaceId"
      },
    };
    for (String[] c : cases) {
      send(ana, post(url, trip.id()), c[0])
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.errors[0].field").value(c[1]));
    }

    // Viagem sem datas: não há dias para planejar.
    long noDates =
        id(
            send(ana, post("/api/v1/trips"), "{\"name\": \"Sem datas\", \"status\": \"PLANNING\"}")
                .andExpect(status().isCreated()));
    send(ana, post(url, noDates), "{\"day\": \"2027-03-12\", \"title\": \"X\"}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("day"));
  }

  @Test
  void activitiesOutsideNewTripDatesAreListedApart() throws Exception {
    User ana = newUser();
    Trip trip = createTrip(ana);
    addActivity(ana, trip.id(), "{\"day\": \"2027-03-16\", \"title\": \"Cascais\"}");

    send(
            ana,
            put("/api/v1/trips/{t}", trip.id()),
            """
            {"name": "Portugal", "status": "PLANNING", "startDate": "2027-03-10",
             "endDate": "2027-03-15", "version": 0}
            """)
        .andExpect(status().isOk());

    fetch(ana, "/api/v1/trips/{t}/itinerary", trip.id())
        .andExpect(jsonPath("$.days.length()").value(6))
        .andExpect(jsonPath("$.outsideActivities.length()").value(1))
        .andExpect(jsonPath("$.outsideActivities[0].title").value("Cascais"))
        .andExpect(jsonPath("$.outsideActivities[0].day").value("2027-03-16"));
  }

  @Test
  void roles() throws Exception {
    User ana = newUser();
    User bia = newUser();
    User caio = newUser();
    User stranger = newUser();
    Trip trip = createTrip(ana);
    addMember(trip.id(), bia, TripRole.EDITOR);
    addMember(trip.id(), caio, TripRole.VIEWER);
    String json = "{\"day\": \"2027-03-12\", \"title\": \"Sintra\"}";

    long id = addActivity(bia, trip.id(), json);
    fetch(caio, "/api/v1/trips/{t}/activities", trip.id())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1));
    send(caio, post("/api/v1/trips/{t}/activities", trip.id()), json)
        .andExpect(status().isForbidden());
    send(
            caio,
            put("/api/v1/trips/{t}/activities/reorder", trip.id()),
            "{\"day\": \"2027-03-12\", \"activityIds\": [%d]}".formatted(id))
        .andExpect(status().isForbidden());
    fetch(stranger, "/api/v1/trips/{t}/activities", trip.id()).andExpect(status().isNotFound());
    send(
            stranger,
            put("/api/v1/trips/{t}/activities/{a}", trip.id(), id),
            "{\"version\": 0, \"day\": \"2027-03-12\", \"title\": \"X\"}")
        .andExpect(status().isNotFound());
  }
}
