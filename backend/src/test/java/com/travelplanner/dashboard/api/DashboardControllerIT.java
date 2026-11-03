package com.travelplanner.dashboard.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

class DashboardControllerIT extends IntegrationTest {

  /** Usuários novos não têm cidade de origem: "hoje" é no fuso padrão. */
  private static final LocalDate TODAY = LocalDate.now(ZoneId.of("America/Sao_Paulo"));

  @Autowired MockMvc mvc;
  @Autowired AppUserService users;

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

  private long trip(User user, String name, LocalDate start, LocalDate end) throws Exception {
    return create(
        user,
        "/api/v1/trips",
        """
        {"name": "%s", "status": "PLANNING", "startDate": %s, "endDate": %s}
        """
            .formatted(name, quoted(start), quoted(end)));
  }

  private static String quoted(LocalDate date) {
    return date == null ? "null" : "\"" + date + "\"";
  }

  private long item(User user, long tripId, String title, LocalDate due) throws Exception {
    return create(
        user,
        "/api/v1/trips/{t}/checklist",
        "{\"category\": \"DOCUMENTS\", \"title\": \"%s\", \"dueDate\": %s}"
            .formatted(title, quoted(due)),
        tripId);
  }

  private void markDone(User user, long tripId, long itemId) throws Exception {
    send(
            user,
            patch("/api/v1/trips/{t}/checklist/{i}/status", tripId, itemId),
            "{\"status\": \"DONE\"}")
        .andExpect(status().isOk());
  }

  private ResultActions dashboard(User user) throws Exception {
    return mvc.perform(get("/api/v1/dashboard").with(user.session())).andExpect(status().isOk());
  }

  @Test
  void emptyDashboardForANewUser() throws Exception {
    dashboard(newUser())
        .andExpect(jsonPath("$.upcomingTrips").isEmpty())
        .andExpect(jsonPath("$.alerts").isEmpty())
        .andExpect(jsonPath("$.pastTrips").isEmpty());
  }

  @Test
  void oneTripWithProgressCountdownAndAlerts() throws Exception {
    User ana = newUser();
    LocalDate start = TODAY.plusDays(12);
    LocalDate end = TODAY.plusDays(20);
    long tripId = trip(ana, "Portugal", start, end);
    create(
        ana,
        "/api/v1/trips/{t}/stops",
        """
        {"cityName": "Lisboa", "countryCode": "PT", "timezone": "Europe/Lisbon",
         "arrivalDate": "%s", "departureDate": "%s"}
        """
            .formatted(start, end),
        tripId);
    long overdue = item(ana, tripId, "Tirar visto", TODAY.minusDays(1));
    item(ana, tripId, "Reservar museu", TODAY.plusDays(5));
    item(ana, tripId, "Comprar adaptador", TODAY.plusDays(30));
    long done = item(ana, tripId, "Imprimir reservas", TODAY.plusDays(2));
    markDone(ana, tripId, done);
    // Passaporte vencendo antes de 6 meses depois da volta → sugestão CRITICAL.
    String travelers =
        body(mvc.perform(get("/api/v1/trips/{t}/travelers", tripId).with(ana.session())));
    send(
            ana,
            put(
                "/api/v1/travelers/{id}",
                ((Number) JsonPath.read(travelers, "$[0].id")).longValue()),
            """
            {"version": %d, "name": "Ana", "ageGroup": "ADULT", "nationalities": ["BR"],
             "passportExpiry": "%s"}
            """
                .formatted(
                    ((Number) JsonPath.read(travelers, "$[0].version")).longValue(),
                    TODAY.plusDays(60)))
        .andExpect(status().isOk());

    String json =
        body(
            dashboard(ana)
                .andExpect(jsonPath("$.upcomingTrips.length()").value(1))
                .andExpect(jsonPath("$.upcomingTrips[0].id").value(tripId))
                .andExpect(jsonPath("$.upcomingTrips[0].daysUntilStart").value(12))
                .andExpect(jsonPath("$.upcomingTrips[0].destinationCount").value(1))
                .andExpect(jsonPath("$.upcomingTrips[0].checklistTotal").value(5))
                .andExpect(jsonPath("$.upcomingTrips[0].checklistDone").value(1))
                .andExpect(jsonPath("$.upcomingTrips[0].pendingSuggestions.critical").value(1))
                .andExpect(jsonPath("$.pastTrips").isEmpty()));
    // Item vencido primeiro, depois a sugestão crítica, depois os prazos por data (o de 30 dias e
    // o feito ficam fora). O 5º item do checklist é o "Renovar passaporte" automático, sem prazo.
    List<String> kinds = JsonPath.read(json, "$.alerts[*].kind");
    List<String> titles =
        JsonPath.read(json, "$.alerts[?(@.kind == 'CHECKLIST_ITEM')].checklistItem.title");
    assertThat(titles)
        .contains("Tirar visto", "Reservar museu")
        .doesNotContain("Comprar adaptador", "Imprimir reservas");
    assertThat((String) JsonPath.read(json, "$.alerts[0].checklistItem.title"))
        .isEqualTo("Tirar visto");
    assertThat((Boolean) JsonPath.read(json, "$.alerts[0].overdue")).isTrue();
    int critical = kinds.indexOf("SUGGESTION");
    assertThat((String) JsonPath.read(json, "$.alerts[" + critical + "].suggestion.ruleCode"))
        .isEqualTo("PASSAPORTE_VALIDADE");
    assertThat((String) JsonPath.read(json, "$.alerts[" + critical + "].severity"))
        .isEqualTo("CRITICAL");
    assertThat(titles.indexOf("Reservar museu")).isGreaterThan(-1);
    List<Boolean> overdueFlags = JsonPath.read(json, "$.alerts[*].overdue");
    assertThat(overdueFlags.subList(critical, overdueFlags.size())).doesNotContain(true);
    assertThat(kinds.lastIndexOf("CHECKLIST_ITEM")).isGreaterThan(critical);

    // Reflete as mudanças: o item feito sai dos alertas e conta no progresso.
    markDone(ana, tripId, overdue);
    dashboard(ana)
        .andExpect(jsonPath("$.upcomingTrips[0].checklistDone").value(2))
        .andExpect(jsonPath("$.alerts[?(@.checklistItem.title == 'Tirar visto')]").isEmpty());
  }

  @Test
  void severalTripsPastTripsAndLimits() throws Exception {
    User ana = newUser();
    User bia = newUser();
    trip(bia, "De outra pessoa", TODAY.plusDays(3), TODAY.plusDays(5));
    long later = trip(ana, "Depois", TODAY.plusDays(40), TODAY.plusDays(45));
    long soon = trip(ana, "Logo", TODAY.plusDays(2), TODAY.plusDays(4));
    long now = trip(ana, "Agora", TODAY.minusDays(1), TODAY.plusDays(1));
    long undated = trip(ana, "Sem data", null, null);
    for (int i = 1; i <= 6; i++) {
      long past =
          trip(ana, "Passada " + i, TODAY.minusDays(100 - i * 10), TODAY.minusDays(95 - i * 10));
      // Itens vencidos de viagens encerradas não viram alerta.
      item(ana, past, "Esquecido " + i, TODAY.minusDays(3));
    }
    for (int i = 0; i < 12; i++) {
      item(ana, soon, "Tarefa " + i, TODAY.plusDays(i % 3));
    }

    String json =
        body(
            dashboard(ana)
                .andExpect(jsonPath("$.upcomingTrips[0].id").value(now))
                .andExpect(jsonPath("$.upcomingTrips[0].status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.upcomingTrips[0].daysUntilStart").value(-1))
                .andExpect(jsonPath("$.upcomingTrips[1].id").value(soon))
                .andExpect(jsonPath("$.upcomingTrips[1].checklistTotal").value(12))
                .andExpect(jsonPath("$.upcomingTrips[1].checklistDone").value(0))
                .andExpect(jsonPath("$.upcomingTrips[2].id").value(later))
                .andExpect(jsonPath("$.upcomingTrips[3].id").value(undated))
                .andExpect(jsonPath("$.upcomingTrips[3].daysUntilStart").doesNotExist())
                .andExpect(jsonPath("$.upcomingTrips.length()").value(4))
                .andExpect(jsonPath("$.alerts.length()").value(10))
                .andExpect(jsonPath("$.pastTrips.length()").value(5))
                .andExpect(jsonPath("$.pastTrips[0].name").value("Passada 6"))
                .andExpect(jsonPath("$.pastTrips[4].name").value("Passada 2")));
    List<String> alertTrips = JsonPath.read(json, "$.alerts[*].tripName");
    assertThat(alertTrips).containsOnly("Logo");
    List<String> dates = JsonPath.read(json, "$.alerts[*].date");
    assertThat(dates).isSorted();
  }
}
