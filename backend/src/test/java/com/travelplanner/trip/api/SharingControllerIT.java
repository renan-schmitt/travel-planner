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
import com.travelplanner.trip.domain.TripRole;
import com.travelplanner.trip.infra.TripMemberRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

class SharingControllerIT extends IntegrationTest {

  @Autowired MockMvc mvc;
  @Autowired AppUserService users;
  @Autowired TripMemberRepository members;

  private record User(long id, String email, RequestPostProcessor session) {}

  private static String randomEmail() {
    return UUID.randomUUID() + "@example.com";
  }

  private User login(String email) {
    return login(TestUsers.googleUser(email, "Pessoa " + email.substring(0, 4)));
  }

  private User login(OidcUser google) {
    RequestPostProcessor session = TestUsers.loggedIn(users, google);
    AppUser user = users.findByEmail(google.getEmail()).orElseThrow();
    return new User(user.getId(), google.getEmail(), session);
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

  private ResultActions send(User user, MockHttpServletRequestBuilder request) throws Exception {
    return mvc.perform(request.with(user.session()).with(csrf()));
  }

  private static <T> T read(ResultActions result, String path) throws Exception {
    return JsonPath.read(result.andReturn().getResponse().getContentAsString(), path);
  }

  private long createTrip(User user) throws Exception {
    return ((Number)
            read(
                send(
                        user,
                        post("/api/v1/trips"),
                        "{\"name\": \"Lisboa\", \"status\": \"PLANNING\"}")
                    .andExpect(status().isCreated()),
                "$.id"))
        .longValue();
  }

  private ResultActions invite(User owner, long tripId, String email, String role)
      throws Exception {
    return send(
        owner,
        post("/api/v1/trips/{id}/members", tripId),
        "{\"email\": \"%s\", \"role\": \"%s\"}".formatted(email, role));
  }

  private long memberId(User user, long tripId) {
    return members.findByTripIdAndUserId(tripId, user.id()).orElseThrow().getId();
  }

  @Test
  void pendingInviteIsLinkedOnLogin() throws Exception {
    User ana = login(randomEmail());
    long tripId = createTrip(ana);
    String beaEmail = randomEmail();

    invite(ana, tripId, beaEmail.toUpperCase(), "EDITOR")
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.email").value(beaEmail))
        .andExpect(jsonPath("$.pending").value(true))
        .andExpect(jsonPath("$.role").value("EDITOR"));
    invite(ana, tripId, beaEmail, "VIEWER").andExpect(status().isConflict());

    // Bia entra pela primeira vez com o e-mail convidado: vira editora da viagem.
    User bea = login(beaEmail);
    mvc.perform(get("/api/v1/trips").with(bea.session()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(tripId))
        .andExpect(jsonPath("$[0].role").value("EDITOR"));
    send(bea, put("/api/v1/trips/{id}", tripId), "{\"name\": \"x\"}")
        .andExpect(status().isBadRequest()); // editor passa da autorização (falta version)

    mvc.perform(get("/api/v1/trips/{id}/members", tripId).with(bea.session()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].role").value("OWNER"))
        .andExpect(jsonPath("$[0].me").value(false))
        .andExpect(jsonPath("$[1].email").value(beaEmail))
        .andExpect(jsonPath("$[1].pending").value(false))
        .andExpect(jsonPath("$[1].me").value(true))
        .andExpect(jsonPath("$[1].name").value("Pessoa " + beaEmail.substring(0, 4)));
  }

  @Test
  void inviteOfExistingUserLinksImmediately() throws Exception {
    User ana = login(randomEmail());
    User bea = login(randomEmail());
    long tripId = createTrip(ana);

    invite(ana, tripId, bea.email(), "VIEWER")
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.pending").value(false));
    invite(ana, tripId, bea.email(), "EDITOR").andExpect(status().isConflict());
    invite(ana, tripId, ana.email(), "EDITOR").andExpect(status().isConflict());

    // Leitora vê, mas não edita nem convida.
    mvc.perform(get("/api/v1/trips/{id}", tripId).with(bea.session()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("VIEWER"));
    send(
            bea,
            put("/api/v1/trips/{id}", tripId),
            "{\"version\": 0, \"name\": \"x\", \"status\": \"PLANNING\"}")
        .andExpect(status().isForbidden());
    invite(bea, tripId, randomEmail(), "VIEWER").andExpect(status().isForbidden());
    send(bea, post("/api/v1/trips/{id}/stops", tripId), "{}")
        .andExpect(status().is4xxClientError());
  }

  @Test
  void validation() throws Exception {
    User ana = login(randomEmail());
    long tripId = createTrip(ana);
    invite(ana, tripId, "nao-e-email", "VIEWER").andExpect(status().isBadRequest());
    send(ana, post("/api/v1/trips/{id}/members", tripId), "{\"email\": \"a@b.com\"}")
        .andExpect(status().isBadRequest());
    send(ana, post("/api/v1/trips/{id}/share-links", tripId), "{\"role\": \"OWNER\"}")
        .andExpect(status().isBadRequest());

    // Quem não é membro não vê nada da viagem.
    User intruder = login(randomEmail());
    mvc.perform(get("/api/v1/trips/{id}/members", tripId).with(intruder.session()))
        .andExpect(status().isNotFound());
  }

  @Test
  void changeRoleAndLastOwner() throws Exception {
    User ana = login(randomEmail());
    User bea = login(randomEmail());
    long tripId = createTrip(ana);
    invite(ana, tripId, bea.email(), "VIEWER").andExpect(status().isCreated());
    long anaMember = memberId(ana, tripId);
    long beaMember = memberId(bea, tripId);

    // Último dono não pode ser rebaixado, removido nem sair.
    send(
            ana,
            put("/api/v1/trips/{t}/members/{m}", tripId, anaMember),
            "{\"version\": 0, \"role\": \"EDITOR\"}")
        .andExpect(status().isConflict());
    send(ana, delete("/api/v1/trips/{t}/members/{m}", tripId, anaMember))
        .andExpect(status().isConflict());

    // Leitor não muda papéis.
    send(
            bea,
            put("/api/v1/trips/{t}/members/{m}", tripId, beaMember),
            "{\"version\": 0, \"role\": \"OWNER\"}")
        .andExpect(status().isForbidden());

    send(
            ana,
            put("/api/v1/trips/{t}/members/{m}", tripId, beaMember),
            "{\"version\": 0, \"role\": \"OWNER\"}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("OWNER"))
        .andExpect(jsonPath("$.version").value(1));
    send(
            ana,
            put("/api/v1/trips/{t}/members/{m}", tripId, beaMember),
            "{\"version\": 0, \"role\": \"EDITOR\"}")
        .andExpect(status().isConflict()); // versão divergente

    // Com dois donos, Ana pode sair; Bia vira a única dona.
    send(ana, delete("/api/v1/trips/{t}/members/{m}", tripId, anaMember))
        .andExpect(status().isNoContent());
    mvc.perform(get("/api/v1/trips/{id}", tripId).with(ana.session()))
        .andExpect(status().isNotFound());
    send(bea, delete("/api/v1/trips/{t}/members/{m}", tripId, beaMember))
        .andExpect(status().isConflict());
  }

  @Test
  void memberCanLeaveButNotRemoveOthers() throws Exception {
    User ana = login(randomEmail());
    User bea = login(randomEmail());
    User caio = login(randomEmail());
    long tripId = createTrip(ana);
    invite(ana, tripId, bea.email(), "EDITOR").andExpect(status().isCreated());
    invite(ana, tripId, caio.email(), "VIEWER").andExpect(status().isCreated());
    String pending = randomEmail();
    long pendingId = ((Number) read(invite(ana, tripId, pending, "VIEWER"), "$.id")).longValue();

    send(bea, delete("/api/v1/trips/{t}/members/{m}", tripId, memberId(caio, tripId)))
        .andExpect(status().isForbidden());
    send(bea, delete("/api/v1/trips/{t}/members/{m}", tripId, memberId(bea, tripId)))
        .andExpect(status().isNoContent());
    // Dono cancela o convite pendente.
    send(ana, delete("/api/v1/trips/{t}/members/{m}", tripId, pendingId))
        .andExpect(status().isNoContent());
    mvc.perform(get("/api/v1/trips/{id}/members", tripId).with(ana.session()))
        .andExpect(jsonPath("$.length()").value(2));
  }

  @Test
  void shareLinkJoinKeepsHigherRoleAndRevokedIs404() throws Exception {
    User ana = login(randomEmail());
    User bea = login(randomEmail());
    User caio = login(randomEmail());
    long tripId = createTrip(ana);
    invite(ana, tripId, caio.email(), "EDITOR").andExpect(status().isCreated());

    ResultActions created =
        send(ana, post("/api/v1/trips/{id}/share-links", tripId), "{\"role\": \"VIEWER\"}")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.role").value("VIEWER"));
    String token = read(created, "$.token");
    long linkId = ((Number) read(created, "$.id")).longValue();
    assertThat(token).hasSize(43).matches("[A-Za-z0-9_-]+");

    // Só o dono gerencia links.
    mvc.perform(get("/api/v1/trips/{id}/share-links", tripId).with(caio.session()))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/v1/trips/{id}/share-links", tripId).with(ana.session()))
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].token").value(token));

    send(bea, post("/api/v1/share-links/{t}/join", token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tripId").value(tripId))
        .andExpect(jsonPath("$.role").value("VIEWER"));
    // Entrar de novo não duplica; editor continua editor.
    send(bea, post("/api/v1/share-links/{t}/join", token)).andExpect(status().isOk());
    send(caio, post("/api/v1/share-links/{t}/join", token))
        .andExpect(jsonPath("$.role").value("EDITOR"));
    assertThat(members.findByTripIdAndUserId(tripId, bea.id()).orElseThrow().getRole())
        .isEqualTo(TripRole.VIEWER);

    send(ana, delete("/api/v1/trips/{t}/share-links/{l}", tripId, linkId))
        .andExpect(status().isNoContent());
    User dani = login(randomEmail());
    send(dani, post("/api/v1/share-links/{t}/join", token)).andExpect(status().isNotFound());
    send(dani, post("/api/v1/share-links/{t}/join", "inexistente"))
        .andExpect(status().isNotFound());
    mvc.perform(get("/api/v1/trips/{id}/share-links", tripId).with(ana.session()))
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void linkThenInviteKeepsSingleMembership() throws Exception {
    User ana = login(randomEmail());
    long tripId = createTrip(ana);
    String beaEmail = randomEmail();
    invite(ana, tripId, beaEmail, "EDITOR").andExpect(status().isCreated());
    String token =
        read(
            send(ana, post("/api/v1/trips/{id}/share-links", tripId), "{\"role\": \"VIEWER\"}"),
            "$.token");

    // Primeiro login já vincula o convite; o link não rebaixa.
    User bea = login(beaEmail);
    send(bea, post("/api/v1/share-links/{t}/join", token))
        .andExpect(jsonPath("$.role").value("EDITOR"));
    mvc.perform(get("/api/v1/trips/{id}/members", tripId).with(ana.session()))
        .andExpect(jsonPath("$.length()").value(2));
  }
}
