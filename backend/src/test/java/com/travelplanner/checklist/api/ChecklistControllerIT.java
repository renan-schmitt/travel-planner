package com.travelplanner.checklist.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

class ChecklistControllerIT extends IntegrationTest {

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

  private long createTrip(User user) throws Exception {
    return id(
        send(user, post("/api/v1/trips"), "{\"name\": \"Portugal\", \"status\": \"PLANNING\"}")
            .andExpect(status().isCreated()));
  }

  private long addItem(User user, long tripId, String category, String title, String extra)
      throws Exception {
    String json =
        """
        {"category": "%s", "title": "%s" %s}
        """
            .formatted(category, title, extra);
    return id(
        send(user, post("/api/v1/trips/{t}/checklist", tripId), json)
            .andExpect(status().isCreated()));
  }

  private void addMember(long tripId, User user, TripRole role) {
    members.save(
        TripMember.of(trips.findById(tripId).orElseThrow(), user.id(), role, Instant.now()));
  }

  private long memberId(User user, long tripId) {
    return members.findByTripIdAndUserId(tripId, user.id()).orElseThrow().getId();
  }

  @Test
  void createListAndFilter() throws Exception {
    User ana = newUser();
    User bea = newUser();
    long tripId = createTrip(ana);
    addMember(tripId, bea, TripRole.EDITOR);

    send(
            ana,
            post("/api/v1/trips/{t}/checklist", tripId),
            """
            {"category": "DOCUMENTS", "title": " Renovar passaporte ", "notes": " ",
             "dueDate": "2027-01-10", "assigneeUserId": %d}
            """
                .formatted(ana.id()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.title").value("Renovar passaporte"))
        .andExpect(jsonPath("$.notes").doesNotExist())
        .andExpect(jsonPath("$.dueDate").value("2027-01-10"))
        .andExpect(jsonPath("$.status").value("OPEN"))
        .andExpect(jsonPath("$.source").value("MANUAL"))
        .andExpect(jsonPath("$.assigneeUserId").value(ana.id()))
        .andExpect(jsonPath("$.sortOrder").value(0))
        .andExpect(jsonPath("$.version").value(0));
    addItem(ana, tripId, "HOME", "Regar plantas", ", \"assigneeUserId\": " + bea.id());
    long gas = addItem(ana, tripId, "HOME", "Desligar o gás", "");
    send(
            ana,
            patch("/api/v1/trips/{t}/checklist/{i}/status", tripId, gas),
            "{\"status\": \"DONE\"}")
        .andExpect(status().isOk());

    mvc.perform(get("/api/v1/trips/{t}/checklist", tripId).with(ana.session()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(3))
        .andExpect(jsonPath("$[1].title").value("Regar plantas"))
        .andExpect(jsonPath("$[2].sortOrder").value(2));
    mvc.perform(
            get("/api/v1/trips/{t}/checklist", tripId)
                .param("category", "HOME")
                .with(ana.session()))
        .andExpect(jsonPath("$.length()").value(2));
    mvc.perform(
            get("/api/v1/trips/{t}/checklist", tripId).param("status", "OPEN").with(ana.session()))
        .andExpect(jsonPath("$.length()").value(2));
    mvc.perform(
            get("/api/v1/trips/{t}/checklist", tripId).param("assignee", "me").with(bea.session()))
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].title").value("Regar plantas"));
    mvc.perform(
            get("/api/v1/trips/{t}/checklist", tripId).param("assignee", "x").with(ana.session()))
        .andExpect(status().isBadRequest());
    mvc.perform(
            get("/api/v1/trips/{t}/checklist", tripId)
                .param("category", "NOPE")
                .with(ana.session()))
        .andExpect(status().isBadRequest());
  }

  @Test
  void statusIsIdempotent() throws Exception {
    User ana = newUser();
    User bea = newUser();
    long tripId = createTrip(ana);
    addMember(tripId, bea, TripRole.EDITOR);
    long item = addItem(ana, tripId, "MONEY", "Avisar o banco", "");

    send(
            ana,
            patch("/api/v1/trips/{t}/checklist/{i}/status", tripId, item),
            "{\"status\": \"DONE\"}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("DONE"))
        .andExpect(jsonPath("$.doneByUserId").value(ana.id()))
        .andExpect(jsonPath("$.doneAt").exists())
        .andExpect(jsonPath("$.version").value(1));
    // Repetir (ex.: fila offline reenviando, ou outra pessoa marcando ao mesmo tempo) não muda
    // nada.
    send(
            bea,
            patch("/api/v1/trips/{t}/checklist/{i}/status", tripId, item),
            "{\"status\": \"DONE\"}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.doneByUserId").value(ana.id()))
        .andExpect(jsonPath("$.version").value(1));
    send(
            bea,
            patch("/api/v1/trips/{t}/checklist/{i}/status", tripId, item),
            "{\"status\": \"OPEN\"}")
        .andExpect(jsonPath("$.status").value("OPEN"))
        .andExpect(jsonPath("$.doneByUserId").doesNotExist())
        .andExpect(jsonPath("$.doneAt").doesNotExist());
    send(ana, patch("/api/v1/trips/{t}/checklist/{i}/status", tripId, item), "{}")
        .andExpect(status().isBadRequest());
  }

  @Test
  void updateConflictAndDelete() throws Exception {
    User ana = newUser();
    long tripId = createTrip(ana);
    long item = addItem(ana, tripId, "OTHER", "Comprar adaptador", "");

    send(
            ana,
            put("/api/v1/trips/{t}/checklist/{i}", tripId, item),
            """
            {"version": 0, "category": "PACKING", "title": "Adaptador de tomada",
             "notes": "Tipo F", "dueDate": "2027-03-01"}
            """)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.category").value("PACKING"))
        .andExpect(jsonPath("$.notes").value("Tipo F"))
        .andExpect(jsonPath("$.version").value(1));
    send(
            ana,
            put("/api/v1/trips/{t}/checklist/{i}", tripId, item),
            "{\"version\": 0, \"category\": \"PACKING\", \"title\": \"X\"}")
        .andExpect(status().isConflict());

    send(ana, delete("/api/v1/trips/{t}/checklist/{i}", tripId, item), "")
        .andExpect(status().isNoContent());
    send(ana, delete("/api/v1/trips/{t}/checklist/{i}", tripId, item), "")
        .andExpect(status().isNotFound());
  }

  @Test
  void validation() throws Exception {
    User ana = newUser();
    User stranger = newUser();
    long tripId = createTrip(ana);

    send(ana, post("/api/v1/trips/{t}/checklist", tripId), "{\"title\": \" \"}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.length()").value(2));
    send(
            ana,
            post("/api/v1/trips/{t}/checklist", tripId),
            "{\"category\": \"OTHER\", \"title\": \"x\", \"assigneeUserId\": %d}"
                .formatted(stranger.id()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("assigneeUserId"));
  }

  @Test
  void reorder() throws Exception {
    User ana = newUser();
    long tripId = createTrip(ana);
    long a = addItem(ana, tripId, "OTHER", "A", "");
    long b = addItem(ana, tripId, "OTHER", "B", "");
    long c = addItem(ana, tripId, "OTHER", "C", "");

    send(
            ana,
            put("/api/v1/trips/{t}/checklist/order", tripId),
            "{\"itemIds\": [%d, %d, %d]}".formatted(c, a, b))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(c));
    mvc.perform(get("/api/v1/trips/{t}/checklist", tripId).with(ana.session()))
        .andExpect(jsonPath("$[0].title").value("C"))
        .andExpect(jsonPath("$[1].title").value("A"))
        .andExpect(jsonPath("$[2].title").value("B"));
    // Novo item entra no fim.
    addItem(ana, tripId, "OTHER", "D", "");
    mvc.perform(get("/api/v1/trips/{t}/checklist", tripId).with(ana.session()))
        .andExpect(jsonPath("$[3].title").value("D"));

    send(
            ana,
            put("/api/v1/trips/{t}/checklist/order", tripId),
            "{\"itemIds\": [%d, %d]}".formatted(a, a))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("itemIds"));
  }

  @Test
  void roles() throws Exception {
    User ana = newUser();
    User viewer = newUser();
    User stranger = newUser();
    long tripId = createTrip(ana);
    addMember(tripId, viewer, TripRole.VIEWER);
    long item = addItem(ana, tripId, "OTHER", "Item", "");

    mvc.perform(get("/api/v1/trips/{t}/checklist", tripId).with(viewer.session()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1));
    send(
            viewer,
            post("/api/v1/trips/{t}/checklist", tripId),
            "{\"category\":\"OTHER\",\"title\":\"x\"}")
        .andExpect(status().isForbidden());
    send(
            viewer,
            patch("/api/v1/trips/{t}/checklist/{i}/status", tripId, item),
            "{\"status\": \"DONE\"}")
        .andExpect(status().isForbidden());
    send(viewer, delete("/api/v1/trips/{t}/checklist/{i}", tripId, item), "")
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/v1/trips/{t}/checklist", tripId).with(stranger.session()))
        .andExpect(status().isNotFound());
    send(
            stranger,
            patch("/api/v1/trips/{t}/checklist/{i}/status", tripId, item),
            "{\"status\": \"DONE\"}")
        .andExpect(status().isNotFound());
  }

  @Test
  void leavingTheTripUnassignsAndDeletingTheTripCascades() throws Exception {
    User ana = newUser();
    User bea = newUser();
    long tripId = createTrip(ana);
    addMember(tripId, bea, TripRole.EDITOR);
    long item = addItem(ana, tripId, "OTHER", "Item", ", \"assigneeUserId\": " + bea.id());

    send(bea, delete("/api/v1/trips/{t}/members/{m}", tripId, memberId(bea, tripId)), "")
        .andExpect(status().isNoContent());
    mvc.perform(get("/api/v1/trips/{t}/checklist", tripId).with(ana.session()))
        .andExpect(jsonPath("$[0].id").value(item))
        .andExpect(jsonPath("$[0].assigneeUserId").doesNotExist())
        .andExpect(jsonPath("$[0].version").value(1));

    send(ana, delete("/api/v1/trips/{t}", tripId), "").andExpect(status().isNoContent());
    mvc.perform(get("/api/v1/trips/{t}/checklist", tripId).with(ana.session()))
        .andExpect(status().isNotFound());
  }
}
