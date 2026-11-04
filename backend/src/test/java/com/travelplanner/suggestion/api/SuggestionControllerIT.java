package com.travelplanner.suggestion.api;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.travelplanner.suggestion.domain.SuggestionEngine;
import com.travelplanner.trip.domain.TripMember;
import com.travelplanner.trip.domain.TripRole;
import com.travelplanner.trip.infra.TripMemberRepository;
import com.travelplanner.trip.infra.TripRepository;
import java.time.Instant;
import java.time.LocalDate;
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

class SuggestionControllerIT extends IntegrationTest {

  @Autowired MockMvc mvc;
  @Autowired AppUserService users;
  @Autowired TripRepository trips;
  @Autowired TripMemberRepository members;
  @Autowired SuggestionEngine engine;
  @Autowired JdbcTemplate jdbc;

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

  private static String body(ResultActions result) throws Exception {
    return result.andReturn().getResponse().getContentAsString();
  }

  private long create(User user, String path, String json, Object... vars) throws Exception {
    String body = body(send(user, post(path, vars), json).andExpect(status().isCreated()));
    return ((Number) JsonPath.read(body, "$.id")).longValue();
  }

  private long trip(User user) throws Exception {
    return create(user, "/api/v1/trips", "{\"name\": \"Portugal\", \"status\": \"PLANNING\"}");
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

  /** Pendentes como "REGRA:chave". */
  private List<String> pending(User user, long tripId) throws Exception {
    return keys(user, tripId, "PENDING");
  }

  private List<String> keys(User user, long tripId, String status) throws Exception {
    String body =
        body(
            mvc.perform(
                    get("/api/v1/trips/{t}/suggestions", tripId)
                        .param("status", status)
                        .with(user.session()))
                .andExpect(status().isOk()));
    List<Map<String, Object>> list = JsonPath.read(body, "$");
    return list.stream().map(s -> s.get("ruleCode") + ":" + s.get("logicalKey")).toList();
  }

  private long suggestionId(User user, long tripId, String ruleCode) throws Exception {
    String body =
        body(mvc.perform(get("/api/v1/trips/{t}/suggestions", tripId).with(user.session())));
    List<Number> ids = JsonPath.read(body, "$[?(@.ruleCode == '" + ruleCode + "')].id");
    return ids.getFirst().longValue();
  }

  private ResultActions decide(User user, long tripId, long id, String json) throws Exception {
    return send(user, patch("/api/v1/trips/{t}/suggestions/{s}", tripId, id), json);
  }

  @Test
  void gapsAppearAndDisappearWhenTheDataIsAdded() throws Exception {
    User ana = newUser();
    long tripId = trip(ana);
    long lisbon = stop(ana, tripId, "Lisboa", "2027-03-11", "2027-03-14");
    String eu = String.valueOf(selfTraveler(ana, tripId).id());

    mvc.perform(get("/api/v1/trips/{t}/suggestions", tripId).with(ana.session()))
        .andExpect(status().isOk())
        // Mais graves primeiro: o seguro (Schengen) e as lacunas de logística são WARNING.
        .andExpect(jsonPath("$[0].severity").value("WARNING"))
        .andExpect(jsonPath("$[?(@.ruleCode == 'SEGURO_VIAGEM')].params.schengen").value(true))
        .andExpect(
            jsonPath("$[?(@.ruleCode == 'SEGURO_VIAGEM')].checklist.title")
                .value("Contratar seguro viagem"));
    String all =
        body(mvc.perform(get("/api/v1/trips/{t}/suggestions", tripId).with(ana.session())));
    List<String> nightCities =
        JsonPath.read(all, "$[?(@.ruleCode == 'NOITE_SEM_HOSPEDAGEM')].params.city");
    assertThat(nightCities).containsExactly("Lisboa", "Lisboa", "Lisboa");
    assertThat(pending(ana, tripId))
        .containsExactlyInAnyOrder(
            "SEGURO_VIAGEM:trip",
            "DESTINO_SEM_TRANSPORTE:home-" + lisbon,
            "DESTINO_SEM_TRANSPORTE:" + lisbon + "-home",
            "NOITE_SEM_HOSPEDAGEM:2027-03-11",
            "NOITE_SEM_HOSPEDAGEM:2027-03-12",
            "NOITE_SEM_HOSPEDAGEM:2027-03-13",
            "TRANSPORTE_LOCAL:" + lisbon,
            "PASSAPORTE_VALIDADE:" + eu,
            "RESTAURANTES:" + lisbon,
            "ATRACOES:" + lisbon,
            "DESTINO_SEM_REQUISITOS:PT");
    mvc.perform(get("/api/v1/trips").with(ana.session()))
        .andExpect(jsonPath("$[?(@.id == " + tripId + ")].pendingSuggestions").value(11));

    create(
        ana,
        "/api/v1/trips/{t}/stops/{s}/lodgings",
        "{\"kind\": \"HOTEL\", \"name\": \"Avenida\", \"checkIn\": \"2027-03-11\","
            + " \"checkOut\": \"2027-03-14\"}",
        tripId,
        lisbon);
    long flight =
        create(
            ana,
            "/api/v1/trips/{t}/segments",
            """
            {"type": "FLIGHT", "destinationStopId": %d,
             "departAt": "2027-03-10T22:40", "departTz": "America/Sao_Paulo",
             "arriveAt": "2027-03-11T11:15", "arriveTz": "Europe/Lisbon"}
            """
                .formatted(lisbon),
            tripId);

    assertThat(pending(ana, tripId))
        .doesNotContain("NOITE_SEM_HOSPEDAGEM:2027-03-11", "DESTINO_SEM_TRANSPORTE:home-" + lisbon)
        .contains("TRANSFER_CHEGADA:" + flight, "TRANSFER_SAIDA:" + flight);

    create(
        ana,
        "/api/v1/trips/{t}/segments",
        """
        {"type": "TRANSFER", "localStopId": %d,
         "departAt": "2027-03-11T12:30", "departTz": "Europe/Lisbon"}
        """
            .formatted(lisbon),
        tripId);
    assertThat(pending(ana, tripId)).doesNotContain("TRANSFER_CHEGADA:" + flight);
  }

  @Test
  void dismissedStaysDismissedAndExpiredSnoozeComesBack() throws Exception {
    User ana = newUser();
    long tripId = trip(ana);
    long lisbon = stop(ana, tripId, "Lisboa", "2027-03-11", "2027-03-14");
    long local = suggestionId(ana, tripId, "TRANSPORTE_LOCAL");
    long insurance = suggestionId(ana, tripId, "SEGURO_VIAGEM");

    decide(ana, tripId, local, "{\"status\": \"DISMISSED\"}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("DISMISSED"));
    String later = LocalDate.now().plusDays(10).toString();
    decide(ana, tripId, insurance, "{\"status\": \"SNOOZED\", \"snoozedUntil\": \"" + later + "\"}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.snoozedUntil").value(later));

    // Outra alteração na viagem roda o motor de novo.
    stop(ana, tripId, "Porto", "2027-03-14", "2027-03-16");
    assertThat(pending(ana, tripId))
        .doesNotContain("TRANSPORTE_LOCAL:" + lisbon, "SEGURO_VIAGEM:trip");
    assertThat(keys(ana, tripId, "DISMISSED")).containsExactly("TRANSPORTE_LOCAL:" + lisbon);
    assertThat(keys(ana, tripId, "SNOOZED")).containsExactly("SEGURO_VIAGEM:trip");

    // O prazo do adiamento chega: a leitura já mostra como pendente, e o motor grava PENDING.
    jdbc.update(
        "update suggestion set snoozed_until = ? where id = ?",
        LocalDate.now().minusDays(1),
        insurance);
    assertThat(pending(ana, tripId)).contains("SEGURO_VIAGEM:trip");
    engine.run(tripId);
    assertThat(
            jdbc.queryForObject(
                "select status from suggestion where id = ?", String.class, insurance))
        .isEqualTo("PENDING");

    // A dispensada que a regra deixa de produzir não é apagada (a decisão fica registrada).
    create(
        ana,
        "/api/v1/trips/{t}/segments",
        """
        {"type": "CAR_RENTAL", "localStopId": %d,
         "departAt": "2027-03-11T12:00", "departTz": "Europe/Lisbon"}
        """
            .formatted(lisbon),
        tripId);
    assertThat(keys(ana, tripId, "DISMISSED")).containsExactly("TRANSPORTE_LOCAL:" + lisbon);
  }

  @Test
  void acceptingTheInsuranceCreatesTheChecklistItemOnce() throws Exception {
    User ana = newUser();
    long tripId = trip(ana);
    stop(ana, tripId, "Lisboa", "2027-03-11", "2027-03-14");
    long insurance = suggestionId(ana, tripId, "SEGURO_VIAGEM");

    decide(ana, tripId, insurance, "{\"status\": \"ACCEPTED\"}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ACCEPTED"));
    decide(ana, tripId, insurance, "{\"status\": \"ACCEPTED\"}").andExpect(status().isOk());

    mvc.perform(get("/api/v1/trips/{t}/checklist", tripId).with(ana.session()))
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].title").value("Contratar seguro viagem"))
        .andExpect(jsonPath("$[0].source").value("RULE"))
        .andExpect(jsonPath("$[0].category").value("DOCUMENTS"))
        .andExpect(jsonPath("$[0].dueDate").value("2027-02-25"));
    assertThat(keys(ana, tripId, "ACCEPTED")).containsExactly("SEGURO_VIAGEM:trip");
  }

  @Test
  void insuranceItemFromATemplateResolvesTheGap() throws Exception {
    User ana = newUser();
    long tripId = trip(ana);
    stop(ana, tripId, "Lisboa", "2027-03-11", "2027-03-14");
    assertThat(pending(ana, tripId)).contains("SEGURO_VIAGEM:trip");

    create(
        ana,
        "/api/v1/trips/{t}/checklist",
        "{\"category\": \"DOCUMENTS\", \"title\": \"Contratar seguro-viagem\"}",
        tripId);
    assertThat(pending(ana, tripId)).contains("SEGURO_VIAGEM:trip");

    create(
        ana,
        "/api/v1/trips/{t}/checklist",
        "{\"category\": \"DOCUMENTS\", \"title\": \"Seguro viagem da família\"}",
        tripId);
    assertThat(pending(ana, tripId)).doesNotContain("SEGURO_VIAGEM:trip");
  }

  @Test
  void expiringPassportEscalatesADismissedSuggestionAndCreatesTheItem() throws Exception {
    User ana = newUser();
    long tripId = trip(ana);
    stop(ana, tripId, "Lisboa", "2027-03-11", "2027-03-14");
    Traveler eu = selfTraveler(ana, tripId);
    long passport = suggestionId(ana, tripId, "PASSAPORTE_VALIDADE");
    decide(ana, tripId, passport, "{\"status\": \"DISMISSED\"}").andExpect(status().isOk());

    send(
            ana,
            put("/api/v1/travelers/{id}", eu.id()),
            """
            {"version": %d, "name": "%s", "ageGroup": "ADULT", "nationalities": ["BR"],
             "passportExpiry": "2027-06-01"}
            """
                .formatted(eu.version(), eu.name()))
        .andExpect(status().isOk());

    mvc.perform(
            get("/api/v1/trips/{t}/suggestions", tripId)
                .param("status", "PENDING")
                .with(ana.session()))
        .andExpect(jsonPath("$[0].ruleCode").value("PASSAPORTE_VALIDADE"))
        .andExpect(jsonPath("$[0].severity").value("CRITICAL"))
        .andExpect(jsonPath("$[0].params.limit").value("2027-09-14"))
        .andExpect(jsonPath("$[0].checklist.automatic").value(true));
    mvc.perform(get("/api/v1/trips/{t}/checklist", tripId).with(ana.session()))
        .andExpect(jsonPath("$[0].title").value("Renovar passaporte de " + eu.name()))
        .andExpect(jsonPath("$[0].source").value("RULE"));
  }

  @Test
  void rolesAndValidation() throws Exception {
    User ana = newUser();
    User bia = newUser();
    User caio = newUser();
    long tripId = trip(ana);
    stop(ana, tripId, "Lisboa", "2027-03-11", "2027-03-14");
    members.save(
        TripMember.of(
            trips.findById(tripId).orElseThrow(), bia.id(), TripRole.VIEWER, Instant.now()));
    long id = suggestionId(ana, tripId, "SEGURO_VIAGEM");

    assertThat(pending(bia, tripId)).isNotEmpty();
    decide(bia, tripId, id, "{\"status\": \"DISMISSED\"}").andExpect(status().isForbidden());
    mvc.perform(get("/api/v1/trips/{t}/suggestions", tripId).with(caio.session()))
        .andExpect(status().isNotFound());
    decide(ana, tripId, id, "{\"status\": \"SNOOZED\"}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("snoozedUntil"));
    decide(
            ana,
            tripId,
            id,
            "{\"status\": \"SNOOZED\", \"snoozedUntil\": \"" + LocalDate.now() + "\"}")
        .andExpect(status().isBadRequest());
    decide(ana, tripId, 999_999, "{\"status\": \"DISMISSED\"}").andExpect(status().isNotFound());
    long other = trip(ana);
    decide(ana, other, id, "{\"status\": \"DISMISSED\"}").andExpect(status().isNotFound());
  }

  @Test
  void deletingTheTripDeletesItsSuggestions() throws Exception {
    User ana = newUser();
    long tripId = trip(ana);
    stop(ana, tripId, "Lisboa", "2027-03-11", "2027-03-14");
    assertThat(pending(ana, tripId)).isNotEmpty();

    mvc.perform(delete("/api/v1/trips/{t}", tripId).with(ana.session()).with(csrf()))
        .andExpect(status().isNoContent());
    assertThat(
            jdbc.queryForObject(
                "select count(*) from suggestion where trip_id = ?", Integer.class, tripId))
        .isZero();
  }

  private record Traveler(long id, long version, String name) {}

  private Traveler selfTraveler(User user, long tripId) throws Exception {
    String body =
        body(mvc.perform(get("/api/v1/trips/{t}/travelers", tripId).with(user.session())));
    return new Traveler(
        ((Number) JsonPath.read(body, "$[0].id")).longValue(),
        ((Number) JsonPath.read(body, "$[0].version")).longValue(),
        JsonPath.read(body, "$[0].name"));
  }
}
