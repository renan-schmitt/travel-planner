package com.travelplanner.checklist.api;

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
import com.travelplanner.trip.domain.TripMember;
import com.travelplanner.trip.domain.TripRole;
import com.travelplanner.trip.infra.TripMemberRepository;
import com.travelplanner.trip.infra.TripRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

class ChecklistTemplateControllerIT extends IntegrationTest {

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

  private static String body(ResultActions result) throws Exception {
    return result.andReturn().getResponse().getContentAsString();
  }

  private static long id(ResultActions result) throws Exception {
    return ((Number) JsonPath.read(body(result), "$.id")).longValue();
  }

  private long createTrip(User user, String extra) throws Exception {
    return id(
        send(
                user,
                post("/api/v1/trips"),
                "{\"name\": \"Nordeste\", \"status\": \"PLANNING\" %s}".formatted(extra))
            .andExpect(status().isCreated()));
  }

  private long systemTemplate(User user, String name) throws Exception {
    String json = body(mvc.perform(get("/api/v1/checklist-templates").with(user.session())));
    List<Number> ids = JsonPath.read(json, "$[?(@.name == '%s')].id".formatted(name));
    return ids.getFirst().longValue();
  }

  private ResultActions apply(User user, long tripId, long templateId) throws Exception {
    return send(
        user,
        post("/api/v1/trips/{t}/checklist/apply-template", tripId),
        "{\"templateId\": %d}".formatted(templateId));
  }

  private long createTemplate(User user) throws Exception {
    return id(
        send(
                user,
                post("/api/v1/checklist-templates"),
                """
                {"name": " Família ", "description": " ", "icon": "CAR", "items": [
                  {"category": "PACKING", "title": "Cadeirinha", "dueOffsetDays": null},
                  {"category": "BOOKINGS", "title": " Revisão do carro ", "dueOffsetDays": -10}
                ]}
                """)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Família"))
            .andExpect(jsonPath("$.description").doesNotExist())
            .andExpect(jsonPath("$.system").value(false))
            .andExpect(jsonPath("$.items[1].title").value("Revisão do carro"))
            .andExpect(jsonPath("$.items[1].dueOffsetDays").value(-10)));
  }

  @Test
  void listsSystemTemplatesAndOwnOnly() throws Exception {
    User ana = newUser();
    User bea = newUser();
    createTemplate(ana);

    mvc.perform(get("/api/v1/checklist-templates").with(ana.session()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(8))
        .andExpect(jsonPath("$[0].name").value("Praia"))
        .andExpect(jsonPath("$[0].icon").value("BEACH"))
        .andExpect(jsonPath("$[0].system").value(true))
        .andExpect(jsonPath("$[6].name").value("Antes de sair de casa"))
        .andExpect(jsonPath("$[7].name").value("Família"))
        .andExpect(jsonPath("$[7].items.length()").value(2));
    mvc.perform(get("/api/v1/checklist-templates").with(bea.session()))
        .andExpect(jsonPath("$.length()").value(7));

    // Todos os templates do sistema têm entre 8 e 20 itens.
    String json = body(mvc.perform(get("/api/v1/checklist-templates").with(bea.session())));
    List<Integer> sizes = JsonPath.read(json, "$[*].items.length()");
    assertThat(sizes).hasSize(7).allSatisfy(n -> assertThat(n).isBetween(8, 20));
  }

  @Test
  void applyComputesDueDatesFromDeparture() throws Exception {
    User ana = newUser();
    long tripId = createTrip(ana, ", \"startDate\": \"2027-03-10\", \"endDate\": \"2027-03-20\"");
    long international = systemTemplate(ana, "Internacional");

    apply(ana, tripId, international)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.skipped").value(0))
        .andExpect(jsonPath("$.added[0].title").value("Conferir validade do passaporte"))
        .andExpect(jsonPath("$.added[0].dueDate").value("2027-01-09"))
        .andExpect(jsonPath("$.added[0].source").value("TEMPLATE"))
        .andExpect(jsonPath("$.added[0].sortOrder").value(0))
        .andExpect(jsonPath("$.added[?(@.title == 'Avisar banco')].dueDate").value("2027-03-07"))
        .andExpect(jsonPath("$.added[9].title").value("Adaptador de tomada"))
        .andExpect(jsonPath("$.added[9].dueDate").doesNotExist());

    // Sem data de partida, os itens não têm data limite.
    long noDates = createTrip(ana, "");
    apply(ana, noDates, international)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.added[0].dueDate").doesNotExist());
  }

  @Test
  void applyDoesNotDuplicateItems() throws Exception {
    User ana = newUser();
    long tripId = createTrip(ana, ", \"startDate\": \"2027-03-10\"");
    send(
            ana,
            post("/api/v1/trips/{t}/checklist", tripId),
            "{\"category\": \"PACKING\", \"title\": \"  protetor SOLAR (rosto e corpo) \"}")
        .andExpect(status().isCreated());
    long beach = systemTemplate(ana, "Praia");
    long international = systemTemplate(ana, "Internacional");

    String first =
        body(
            apply(ana, tripId, beach)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.skipped").value(1))
                .andExpect(jsonPath("$.added[0].sortOrder").value(1)));
    int beachAdded = JsonPath.read(first, "$.added.length()");
    String second = body(apply(ana, tripId, international).andExpect(status().isOk()));
    int intlAdded = JsonPath.read(second, "$.added.length()");
    // Aplicar de novo não cria nada.
    apply(ana, tripId, beach)
        .andExpect(jsonPath("$.added.length()").value(0))
        .andExpect(jsonPath("$.skipped").value(beachAdded + 1));

    String list = body(mvc.perform(get("/api/v1/trips/{t}/checklist", tripId).with(ana.session())));
    List<String> titles = JsonPath.read(list, "$[*].title");
    assertThat(titles).hasSize(1 + beachAdded + intlAdded).doesNotHaveDuplicates();
  }

  @Test
  void userTemplatesCrudAndSystemReadOnly() throws Exception {
    User ana = newUser();
    User bea = newUser();
    long own = createTemplate(ana);
    long system = systemTemplate(ana, "Neve");
    String update =
        """
        {"version": %d, "name": "Família 2", "icon": "HOME",
         "items": [{"category": "OTHER", "title": "Só um"}]}
        """;

    send(ana, put("/api/v1/checklist-templates/{id}", own), update.formatted(0))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Família 2"))
        .andExpect(jsonPath("$.items.length()").value(1))
        .andExpect(jsonPath("$.version").value(1));
    send(ana, put("/api/v1/checklist-templates/{id}", own), update.formatted(0))
        .andExpect(status().isConflict());
    mvc.perform(get("/api/v1/checklist-templates/{id}", own).with(ana.session()))
        .andExpect(jsonPath("$.items[0].title").value("Só um"));

    // Do sistema: todos leem, ninguém altera.
    mvc.perform(get("/api/v1/checklist-templates/{id}", system).with(bea.session()))
        .andExpect(status().isOk());
    send(ana, put("/api/v1/checklist-templates/{id}", system), update.formatted(0))
        .andExpect(status().isForbidden());
    send(ana, delete("/api/v1/checklist-templates/{id}", system), "")
        .andExpect(status().isForbidden());

    // De outra pessoa: nem existe.
    mvc.perform(get("/api/v1/checklist-templates/{id}", own).with(bea.session()))
        .andExpect(status().isNotFound());
    send(bea, put("/api/v1/checklist-templates/{id}", own), update.formatted(1))
        .andExpect(status().isNotFound());
    long beaTrip = createTrip(bea, "");
    apply(bea, beaTrip, own).andExpect(status().isNotFound());

    // Validação.
    send(
            ana,
            post("/api/v1/checklist-templates"),
            "{\"name\": \"\", \"icon\": \"NOPE\", \"items\": [{\"category\": \"OTHER\"}]}")
        .andExpect(status().isBadRequest());
    send(
            ana,
            post("/api/v1/checklist-templates"),
            "{\"name\": \"X\", \"icon\": \"LIST\", \"items\": [{\"category\": \"OTHER\","
                + " \"title\": \"a\", \"dueOffsetDays\": -400}]}")
        .andExpect(status().isBadRequest());

    send(ana, delete("/api/v1/checklist-templates/{id}", own), "")
        .andExpect(status().isNoContent());
    mvc.perform(get("/api/v1/checklist-templates/{id}", own).with(ana.session()))
        .andExpect(status().isNotFound());
  }

  @Test
  void saveTripChecklistAsTemplate() throws Exception {
    User ana = newUser();
    long tripId = createTrip(ana, ", \"startDate\": \"2027-03-10\"");
    for (String item :
        List.of(
            "{\"category\": \"DOCUMENTS\", \"title\": \"Seguro\", \"dueDate\": \"2027-02-24\"}",
            "{\"category\": \"HOME\", \"title\": \"Regar plantas\"}",
            "{\"category\": \"OTHER\", \"title\": \"Ligar para a vó\", \"dueDate\":"
                + " \"2027-03-12\"}")) {
      send(ana, post("/api/v1/trips/{t}/checklist", tripId), item).andExpect(status().isCreated());
    }

    long template =
        id(
            send(
                    ana,
                    post("/api/v1/trips/{t}/checklist/save-as-template", tripId),
                    "{\"name\": \" Meu padrão \"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Meu padrão"))
                .andExpect(jsonPath("$.icon").value("LIST"))
                .andExpect(jsonPath("$.system").value(false))
                .andExpect(jsonPath("$.items.length()").value(3))
                .andExpect(jsonPath("$.items[0].dueOffsetDays").value(-14))
                .andExpect(jsonPath("$.items[1].dueOffsetDays").doesNotExist())
                .andExpect(jsonPath("$.items[2].dueOffsetDays").value(2)));

    // Aplicado numa viagem que parte em outra data, os prazos acompanham a partida.
    long other = createTrip(ana, ", \"startDate\": \"2027-07-01\"");
    apply(ana, other, template)
        .andExpect(jsonPath("$.added[0].dueDate").value("2027-06-17"))
        .andExpect(jsonPath("$.added[2].dueDate").value("2027-07-03"));

    send(ana, post("/api/v1/trips/{t}/checklist/save-as-template", tripId), "{\"name\": \" \"}")
        .andExpect(status().isBadRequest());
  }

  @Test
  void rolesOnTrip() throws Exception {
    User ana = newUser();
    User vic = newUser();
    User out = newUser();
    long tripId = createTrip(ana, "");
    members.save(
        TripMember.of(
            trips.findById(tripId).orElseThrow(), vic.id(), TripRole.VIEWER, Instant.now()));
    long beach = systemTemplate(ana, "Praia");

    apply(vic, tripId, beach).andExpect(status().isForbidden());
    apply(out, tripId, beach).andExpect(status().isNotFound());
    apply(ana, tripId, 999_999).andExpect(status().isNotFound());
    // Leitor pode guardar o checklist como template próprio.
    send(
            vic,
            post("/api/v1/trips/{t}/checklist/save-as-template", tripId),
            "{\"name\": \"Copiado\"}")
        .andExpect(status().isCreated());
    send(
            out,
            post("/api/v1/trips/{t}/checklist/save-as-template", tripId),
            "{\"name\": \"Copiado\"}")
        .andExpect(status().isNotFound());
  }
}
