package com.travelplanner.auth.dev;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.travelplanner.IntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** Login dev + sessão no MySQL (Spring Session JDBC) + logout, de ponta a ponta no MockMvc. */
class DevLoginIT extends IntegrationTest {

  @Autowired MockMvc mvc;

  @Autowired JdbcTemplate jdbc;

  @Test
  void withoutCsrfReturns403() throws Exception {
    mvc.perform(post("/dev/login").param("email", "gabi@example.com"))
        .andExpect(status().isForbidden());
  }

  @Test
  void invalidEmailReturns400() throws Exception {
    mvc.perform(post("/dev/login").param("email", "não-é-email").with(csrf()))
        .andExpect(status().isBadRequest());
  }

  @Test
  void logsInPersistsSessionAndLogsOut() throws Exception {
    MvcResult login =
        mvc.perform(post("/dev/login").param("email", "hugo@example.com").with(csrf()))
            .andExpect(status().isNoContent())
            .andReturn();
    Cookie session = sessionCookie(login);

    String userId =
        mvc.perform(get("/api/v1/me").cookie(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("hugo@example.com"))
            .andExpect(jsonPath("$.name").value("hugo"))
            .andReturn()
            .getResponse()
            .getContentAsString()
            .replaceAll(".*\"id\":(\\d+).*", "$1");

    // A sessão está no MySQL, então sobrevive a um restart da aplicação.
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM SPRING_SESSION WHERE PRINCIPAL_NAME = ?",
                Integer.class,
                userId))
        .isEqualTo(1);

    mvc.perform(post("/logout").cookie(session).with(csrf())).andExpect(status().isNoContent());

    mvc.perform(get("/api/v1/me").cookie(session)).andExpect(status().isUnauthorized());
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM SPRING_SESSION WHERE PRINCIPAL_NAME = ?",
                Integer.class,
                userId))
        .isZero();
  }

  @Test
  void sameEmailReusesUser() throws Exception {
    String first = loginAndGetUserId("iris@example.com");
    String second = loginAndGetUserId("Iris@Example.com");

    assertThat(second).isEqualTo(first);
  }

  private String loginAndGetUserId(String email) throws Exception {
    Cookie session =
        sessionCookie(
            mvc.perform(post("/dev/login").param("email", email).with(csrf())).andReturn());
    return mvc.perform(get("/api/v1/me").cookie(session))
        .andReturn()
        .getResponse()
        .getContentAsString()
        .replaceAll(".*\"id\":(\\d+).*", "$1");
  }

  /** O Spring Session escreve o cookie {@code SESSION} direto no header {@code Set-Cookie}. */
  private static Cookie sessionCookie(MvcResult result) {
    return result.getResponse().getHeaders(HttpHeaders.SET_COOKIE).stream()
        .filter(header -> header.startsWith("SESSION="))
        .map(header -> header.substring("SESSION=".length(), header.indexOf(';')))
        .map(value -> new Cookie("SESSION", value))
        .findFirst()
        .orElseThrow(() -> new AssertionError("Sem cookie SESSION na resposta"));
  }
}
