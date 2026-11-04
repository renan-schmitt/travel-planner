package com.travelplanner.requirements;

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
import com.travelplanner.suggestion.domain.SuggestionEngine;
import com.travelplanner.trip.domain.TripMember;
import com.travelplanner.trip.domain.TripRole;
import com.travelplanner.trip.infra.TripMemberRepository;
import com.travelplanner.trip.infra.TripRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

class RequirementIT extends IntegrationTest {

  @Autowired MockMvc mvc;
  @Autowired AppUserService users;
  @Autowired TripRepository trips;
  @Autowired TripMemberRepository members;
  @Autowired SuggestionEngine engine;
  @Autowired JdbcTemplate jdbc;

  private record User(long id, RequestPostProcessor session) {}

  private User user(String email) {
    var google = TestUsers.googleUser(email, "Ana");
    AppUser user = users.upsertFromOidc(google);
    return new User(user.getId(), TestUsers.loggedIn(users, google));
  }

  private User newUser() {
    return user(UUID.randomUUID() + "@example.com");
  }

  private User admin() {
    return user("admin@example.com");
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

  private static String body(ResultActions result) throws Exception {
    return result.andReturn().getResponse().getContentAsString();
  }

  private long create(User user, String path, String json, Object... vars) throws Exception {
    String body = body(send(user, post(path, vars), json).andExpect(status().isCreated()));
    return ((Number) JsonPath.read(body, "$.id")).longValue();
  }

  private long trip(User user, String name) throws Exception {
    return create(
        user, "/api/v1/trips", "{\"name\": \"%s\", \"status\": \"PLANNING\"}".formatted(name));
  }

  private void stop(User user, long tripId, String city, String country, String tz)
      throws Exception {
    create(
        user,
        "/api/v1/trips/{t}/stops",
        """
        {"cityName": "%s", "countryCode": "%s", "timezone": "%s",
         "arrivalDate": "2027-03-10", "departureDate": "2027-03-17"}
        """
            .formatted(city, country, tz),
        tripId);
  }

  private List<String> pending(User user, long tripId) throws Exception {
    String body =
        body(
            mvc.perform(
                    get("/api/v1/trips/{t}/suggestions", tripId)
                        .param("status", "PENDING")
                        .with(user.session()))
                .andExpect(status().isOk()));
    List<Map<String, Object>> list = JsonPath.read(body, "$");
    return list.stream().map(s -> s.get("ruleCode") + ":" + s.get("logicalKey")).toList();
  }

  private long seededETicket(User admin) throws Exception {
    String body =
        body(
            mvc.perform(
                    get("/api/v1/admin/requirements")
                        .param("countryCode", "do")
                        .with(admin.session()))
                .andExpect(status().isOk()));
    List<Number> ids = JsonPath.read(body, "$[?(@.type == 'ENTRY_FORM')].id");
    return ids.getFirst().longValue();
  }

  @Test
  void publishingTheDominicanETicketCreatesTheChecklistItemWithDeadlineAndLink() throws Exception {
    User ana = newUser();
    User admin = admin();
    long tripId = trip(ana, "Punta Cana");
    stop(ana, tripId, "Punta Cana", "DO", "America/Santo_Domingo");

    // Seed em rascunho: não vale para a viagem.
    long eticket = seededETicket(admin);
    mvc.perform(get("/api/v1/admin/requirements/{id}", eticket).with(admin.session()))
        .andExpect(jsonPath("$.status").value("DRAFT"))
        .andExpect(jsonPath("$.leadDays").value(3))
        .andExpect(jsonPath("$.verifiedAt").doesNotExist());
    assertThat(pending(ana, tripId))
        .contains("DESTINO_SEM_REQUISITOS:DO")
        .noneMatch(k -> k.startsWith("DOCUMENTO_DESTINO"));
    mvc.perform(get("/api/v1/trips/{t}/requirements", tripId).with(ana.session()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].countryCode").value("DO"))
        .andExpect(jsonPath("$[0].country.name").value("República Dominicana"))
        .andExpect(jsonPath("$[0].country.plugTypes").value("A,B"))
        .andExpect(jsonPath("$[0].requirements").isEmpty())
        .andExpect(jsonPath("$[0].nationalitiesWithoutData[0]").value("BR"));

    try {
      send(admin, post("/api/v1/admin/requirements/{id}/publish", eticket), "")
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.status").value("PUBLISHED"))
          .andExpect(jsonPath("$.verifiedAt").isNotEmpty());

      // O motor recalcula a viagem ao publicar.
      assertThat(pending(ana, tripId))
          .contains("DOCUMENTO_DESTINO:" + eticket)
          .doesNotContain("DESTINO_SEM_REQUISITOS:DO");
      String checklist =
          body(mvc.perform(get("/api/v1/trips/{t}/checklist", tripId).with(ana.session())));
      List<Map<String, Object>> items =
          JsonPath.read(checklist, "$[?(@.ruleCode == 'DOCUMENTO_DESTINO')]");
      assertThat(items)
          .singleElement()
          .satisfies(
              item -> {
                assertThat(item.get("title"))
                    .isEqualTo("Preencher o e-Ticket (República Dominicana)");
                assertThat(item.get("dueDate")).isEqualTo("2027-03-07");
                assertThat((String) item.get("notes"))
                    .contains("https://eticket.migracion.gob.do/");
                assertThat(item.get("category")).isEqualTo("DOCUMENTS");
              });
      mvc.perform(get("/api/v1/trips/{t}/requirements", tripId).with(ana.session()))
          .andExpect(jsonPath("$[0].requirements[0].id").value(eticket))
          .andExpect(jsonPath("$[0].requirements[0].dueDate").value("2027-03-07"))
          .andExpect(
              jsonPath("$[0].requirements[0].officialUrl")
                  .value("https://eticket.migracion.gob.do/"))
          .andExpect(jsonPath("$[0].nationalitiesWithoutData").isEmpty());
    } finally {
      send(admin, post("/api/v1/admin/requirements/{id}/unpublish", eticket), "")
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.status").value("DRAFT"));
    }
    assertThat(pending(ana, tripId))
        .contains("DESTINO_SEM_REQUISITOS:DO")
        .noneMatch(k -> k.startsWith("DOCUMENTO_DESTINO"));
  }

  @Test
  void adminCrudValidationAndVersioning() throws Exception {
    User admin = admin();
    String json =
        """
        {"countryCode": "jp", "nationality": "br", "type": "VISA", "title": "Visto japonês",
         "description": "  ", "leadDays": 20, "officialUrl": "https://www.mofa.go.jp/"}
        """;
    long id = create(admin, "/api/v1/admin/requirements", json);
    String created =
        body(mvc.perform(get("/api/v1/admin/requirements/{id}", id).with(admin.session())));
    assertThat((String) JsonPath.read(created, "$.countryCode")).isEqualTo("JP");
    assertThat((String) JsonPath.read(created, "$.nationality")).isEqualTo("BR");
    assertThat((Object) JsonPath.read(created, "$.description")).isNull();
    assertThat((String) JsonPath.read(created, "$.status")).isEqualTo("DRAFT");
    assertThat((String) JsonPath.read(created, "$.source")).isEqualTo("MANUAL");

    String update =
        """
        {"countryCode": "JP", "nationality": "BR", "type": "OTHER", "title": "Isenção de visto",
         "leadDays": null, "officialUrl": "https://www.mofa.go.jp/", "version": %d}
        """;
    send(admin, put("/api/v1/admin/requirements/{id}", id), update.formatted(0))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.type").value("OTHER"))
        .andExpect(jsonPath("$.version").value(1));
    send(admin, put("/api/v1/admin/requirements/{id}", id), update.formatted(0))
        .andExpect(status().isConflict());

    mvc.perform(
            get("/api/v1/admin/requirements")
                .param("countryCode", "JP")
                .param("status", "DRAFT")
                .with(admin.session()))
        .andExpect(jsonPath("$[?(@.id == " + id + ")].title").value("Isenção de visto"));

    send(
            admin,
            post("/api/v1/admin/requirements"),
            """
            {"countryCode": "ZZ", "nationality": "BR", "type": "VISA", "title": "x",
             "officialUrl": "https://x.example"}
            """)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("countryCode"));
    send(
            admin,
            post("/api/v1/admin/requirements"),
            """
            {"countryCode": "JP", "nationality": "BR", "type": "VISA", "title": " ",
             "leadDays": -1, "officialUrl": "ftp://x"}
            """)
        .andExpect(status().isBadRequest());

    send(admin, delete("/api/v1/admin/requirements/{id}", id), "")
        .andExpect(status().isNoContent());
    mvc.perform(get("/api/v1/admin/requirements/{id}", id).with(admin.session()))
        .andExpect(status().isNotFound());
  }

  @Test
  void onlyAdminsManageTheBaseAndSeeOutdatedRequirements() throws Exception {
    User admin = admin();
    User bia = newUser();
    User caio = newUser();
    mvc.perform(get("/api/v1/admin/requirements").with(bia.session()))
        .andExpect(status().isForbidden());
    send(bia, post("/api/v1/admin/requirements/{id}/publish", 1), "")
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/v1/countries").with(bia.session()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.code == 'PT')].schengen").value(true))
        .andExpect(jsonPath("$[?(@.code == 'AR')].brIdCardAccepted").value(true));

    long tripId = trip(admin, "Islândia");
    stop(admin, tripId, "Reykjavík", "IS", "Atlantic/Reykjavik");
    members.save(
        TripMember.of(
            trips.findById(tripId).orElseThrow(), bia.id(), TripRole.VIEWER, Instant.now()));
    long id =
        create(
            admin,
            "/api/v1/admin/requirements",
            """
            {"countryCode": "IS", "nationality": "BR", "type": "OTHER", "title": "Isenção",
             "officialUrl": "https://island.example"}
            """);
    try {
      send(admin, post("/api/v1/admin/requirements/{id}/publish", id), "")
          .andExpect(status().isOk());
      assertThat(pending(admin, tripId)).doesNotContain("REQUISITO_DESATUALIZADO:" + id);

      jdbc.update("update destination_requirement set verified_at = '2020-01-01' where id = ?", id);
      engine.run(tripId);

      assertThat(pending(admin, tripId)).contains("REQUISITO_DESATUALIZADO:" + id);
      assertThat(pending(bia, tripId))
          .contains("DOCUMENTO_DESTINO:" + id)
          .doesNotContain("REQUISITO_DESATUALIZADO:" + id);
      // A contagem do card é a mesma para todos (sem a regra de admin).
      mvc.perform(get("/api/v1/trips/{t}", tripId).with(bia.session()))
          .andExpect(jsonPath("$.pendingSuggestions").value(pending(bia, tripId).size()));
      mvc.perform(get("/api/v1/trips/{t}/requirements", tripId).with(caio.session()))
          .andExpect(status().isNotFound());
    } finally {
      send(admin, delete("/api/v1/admin/requirements/{id}", id), "")
          .andExpect(status().isNoContent());
    }
    assertThat(pending(admin, tripId)).contains("DESTINO_SEM_REQUISITOS:IS");
  }
}
