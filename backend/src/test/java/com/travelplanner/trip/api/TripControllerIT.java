package com.travelplanner.trip.api;

import static com.travelplanner.auth.TestUsers.loggedIn;
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
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@RecordApplicationEvents
class TripControllerIT extends IntegrationTest {

  /** Fuso padrão de {@code app.time.default-zone} (os usuários de teste não têm cidade). */
  private static final LocalDate TODAY = LocalDate.now(ZoneId.of("America/Sao_Paulo"));

  @Autowired MockMvc mvc;
  @Autowired AppUserService users;
  @Autowired TripRepository trips;
  @Autowired TripMemberRepository members;
  @Autowired ApplicationEvents events;

  /** Usuário novo (e-mail único, porque os testes compartilham o banco). */
  private record User(long id, RequestPostProcessor session) {}

  private User newUser() {
    var google = TestUsers.googleUser(UUID.randomUUID() + "@example.com", "Viajante");
    AppUser user = users.upsertFromOidc(google);
    return new User(user.getId(), loggedIn(users, google));
  }

  private long createTrip(User user, String json) throws Exception {
    String body =
        mvc.perform(
                post("/api/v1/trips")
                    .with(user.session())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return ((Number) JsonPath.read(body, "$.id")).longValue();
  }

  private long createTrip(User user) throws Exception {
    return createTrip(user, "{\"name\": \"Lisboa\", \"status\": \"PLANNING\"}");
  }

  private void addMember(long tripId, User user, TripRole role) {
    members.save(
        TripMember.of(trips.findById(tripId).orElseThrow(), user.id(), role, Instant.now()));
  }

  private String update(long version, String name) {
    return """
        {"version": %d, "name": "%s", "status": "CONFIRMED"}
        """
        .formatted(version, name);
  }

  @Test
  void withoutSessionReturns401() throws Exception {
    mvc.perform(get("/api/v1/trips")).andExpect(status().isUnauthorized());
  }

  @Test
  void createsTripWithOwnerMembership() throws Exception {
    User ana = newUser();

    mvc.perform(
            post("/api/v1/trips")
                .with(ana.session())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name": "  Férias em Portugal ", "startDate": "2099-07-01",
                     "endDate": "2099-07-15", "status": "CONFIRMED", "coverColor": "#7c5cc4"}
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.name").value("Férias em Portugal"))
        .andExpect(jsonPath("$.startDate").value("2099-07-01"))
        .andExpect(jsonPath("$.endDate").value("2099-07-15"))
        .andExpect(jsonPath("$.status").value("CONFIRMED"))
        .andExpect(jsonPath("$.plannedStatus").value("CONFIRMED"))
        .andExpect(jsonPath("$.coverColor").value("#7C5CC4"))
        .andExpect(jsonPath("$.role").value("OWNER"))
        .andExpect(jsonPath("$.version").value(0));

    assertThat(events.stream(TripChangedEvent.class)).hasSize(1);
  }

  @Test
  void assignsPaletteColorsInTurn() throws Exception {
    User ana = newUser();
    long first = createTrip(ana);
    long second = createTrip(ana);

    assertThat(trips.findById(first).orElseThrow().getCoverColor()).isEqualTo("#0E7C86");
    assertThat(trips.findById(second).orElseThrow().getCoverColor()).isEqualTo("#E8613F");
  }

  @Test
  void listsOnlyOwnTripsSplitByScope() throws Exception {
    User ana = newUser();
    User bia = newUser();
    String past =
        """
        {"name": "Passada", "startDate": "%s", "endDate": "%s", "status": "CONFIRMED"}
        """
            .formatted(TODAY.minusDays(20), TODAY.minusDays(10));
    String current =
        """
        {"name": "Agora", "startDate": "%s", "endDate": "%s", "status": "CONFIRMED"}
        """
            .formatted(TODAY.minusDays(1), TODAY.plusDays(1));
    String later =
        """
        {"name": "Depois", "startDate": "%s", "status": "PLANNING"}
        """
            .formatted(TODAY.plusDays(30));
    createTrip(ana, past);
    createTrip(ana, "{\"name\": \"Sem data\", \"status\": \"PLANNING\"}");
    createTrip(ana, later);
    createTrip(ana, current);
    createTrip(bia, "{\"name\": \"Da Bia\", \"status\": \"PLANNING\"}");

    mvc.perform(get("/api/v1/trips").with(ana.session()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(3))
        .andExpect(jsonPath("$[0].name").value("Agora"))
        .andExpect(jsonPath("$[0].status").value("IN_PROGRESS"))
        .andExpect(jsonPath("$[0].role").value("OWNER"))
        .andExpect(jsonPath("$[0].destinationCount").value(0))
        .andExpect(jsonPath("$[1].name").value("Depois"))
        .andExpect(jsonPath("$[2].name").value("Sem data"));

    mvc.perform(get("/api/v1/trips").param("scope", "past").with(ana.session()))
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].name").value("Passada"))
        .andExpect(jsonPath("$[0].status").value("DONE"));

    mvc.perform(get("/api/v1/trips").param("scope", "all").with(ana.session()))
        .andExpect(jsonPath("$.length()").value(4));

    mvc.perform(get("/api/v1/trips").param("scope", "futuro").with(ana.session()))
        .andExpect(status().isBadRequest());
  }

  @Test
  void otherUserCannotSeeOrChangeTheTrip() throws Exception {
    User ana = newUser();
    User intruder = newUser();
    long tripId = createTrip(ana);

    mvc.perform(get("/api/v1/trips/{id}", tripId).with(intruder.session()))
        .andExpect(status().isNotFound());
    mvc.perform(
            put("/api/v1/trips/{id}", tripId)
                .with(intruder.session())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(update(0, "Invadida")))
        .andExpect(status().isNotFound());
    mvc.perform(delete("/api/v1/trips/{id}", tripId).with(intruder.session()).with(csrf()))
        .andExpect(status().isNotFound());
    mvc.perform(get("/api/v1/trips").with(intruder.session()))
        .andExpect(jsonPath("$.length()").value(0));

    assertThat(trips.findById(tripId).orElseThrow().getName()).isEqualTo("Lisboa");
  }

  @Test
  void nonexistentTripReturns404() throws Exception {
    mvc.perform(get("/api/v1/trips/{id}", Long.MAX_VALUE).with(newUser().session()))
        .andExpect(status().isNotFound());
  }

  @Test
  void viewerReadsButCannotEditOrDelete() throws Exception {
    User owner = newUser();
    User viewer = newUser();
    long tripId = createTrip(owner);
    addMember(tripId, viewer, TripRole.VIEWER);

    mvc.perform(get("/api/v1/trips/{id}", tripId).with(viewer.session()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("VIEWER"));
    mvc.perform(get("/api/v1/trips").with(viewer.session()))
        .andExpect(jsonPath("$[0].role").value("VIEWER"));
    mvc.perform(
            put("/api/v1/trips/{id}", tripId)
                .with(viewer.session())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(update(0, "Nova")))
        .andExpect(status().isForbidden());
    mvc.perform(delete("/api/v1/trips/{id}", tripId).with(viewer.session()).with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  void editorEditsButCannotDelete() throws Exception {
    User owner = newUser();
    User editor = newUser();
    long tripId = createTrip(owner);
    addMember(tripId, editor, TripRole.EDITOR);

    mvc.perform(
            put("/api/v1/trips/{id}", tripId)
                .with(editor.session())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(update(0, "Lisboa e Porto")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Lisboa e Porto"))
        .andExpect(jsonPath("$.role").value("EDITOR"));
    mvc.perform(delete("/api/v1/trips/{id}", tripId).with(editor.session()).with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  void updateReplacesFieldsAndKeepsColor() throws Exception {
    User ana = newUser();
    long tripId =
        createTrip(
            ana,
            """
            {"name": "Roma", "startDate": "2099-01-01", "endDate": "2099-01-05",
             "status": "PLANNING", "coverColor": "#C2477A"}
            """);
    events.clear();

    mvc.perform(
            put("/api/v1/trips/{id}", tripId)
                .with(ana.session())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(update(0, "Roma e Florença")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Roma e Florença"))
        .andExpect(jsonPath("$.startDate").doesNotExist())
        .andExpect(jsonPath("$.endDate").doesNotExist())
        .andExpect(jsonPath("$.status").value("CONFIRMED"))
        .andExpect(jsonPath("$.coverColor").value("#C2477A"))
        .andExpect(jsonPath("$.version").value(1));

    assertThat(events.stream(TripChangedEvent.class)).containsExactly(new TripChangedEvent(tripId));
  }

  @Test
  void staleVersionReturns409() throws Exception {
    User ana = newUser();
    long tripId = createTrip(ana);
    var request =
        put("/api/v1/trips/{id}", tripId)
            .with(ana.session())
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON);

    mvc.perform(request.content(update(0, "Primeira"))).andExpect(status().isOk());
    mvc.perform(request.content(update(0, "Segunda"))).andExpect(status().isConflict());

    assertThat(trips.findById(tripId).orElseThrow().getName()).isEqualTo("Primeira");
  }

  @Test
  void validatesFields() throws Exception {
    mvc.perform(
            post("/api/v1/trips")
                .with(newUser().session())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name": " ", "startDate": "2099-07-10", "endDate": "2099-07-01",
                     "status": "PLANNING", "coverColor": "azul"}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[?(@.field == 'name')]").exists())
        .andExpect(jsonPath("$.errors[?(@.field == 'endDate')]").exists())
        .andExpect(jsonPath("$.errors[?(@.field == 'coverColor')]").exists());
  }

  @Test
  void computedStatusCannotBeSet() throws Exception {
    mvc.perform(
            post("/api/v1/trips")
                .with(newUser().session())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"Roma\", \"status\": \"DONE\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void updateRequiresVersion() throws Exception {
    User ana = newUser();
    long tripId = createTrip(ana);

    mvc.perform(
            put("/api/v1/trips/{id}", tripId)
                .with(ana.session())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"Roma\", \"status\": \"PLANNING\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[?(@.field == 'version')]").exists());
  }

  @Test
  void ownerDeletesTripAndMembers() throws Exception {
    User owner = newUser();
    User viewer = newUser();
    long tripId = createTrip(owner);
    addMember(tripId, viewer, TripRole.VIEWER);
    assertThat(members.countByTripId(tripId)).isEqualTo(2);
    events.clear();

    mvc.perform(delete("/api/v1/trips/{id}", tripId).with(owner.session()).with(csrf()))
        .andExpect(status().isNoContent());

    assertThat(trips.findById(tripId)).isEmpty();
    assertThat(members.countByTripId(tripId)).isZero();
    assertThat(events.stream(TripChangedEvent.class)).containsExactly(new TripChangedEvent(tripId));
    mvc.perform(get("/api/v1/trips/{id}", tripId).with(owner.session()))
        .andExpect(status().isNotFound());
  }

  @Test
  void writesRequireCsrf() throws Exception {
    mvc.perform(
            post("/api/v1/trips")
                .with(newUser().session())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"Roma\", \"status\": \"PLANNING\"}"))
        .andExpect(status().isForbidden());
  }
}
