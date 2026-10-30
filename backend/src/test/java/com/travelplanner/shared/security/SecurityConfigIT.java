package com.travelplanner.shared.security;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.travelplanner.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Regras de acesso e cabeçalhos. Os cookies ({@code XSRF-TOKEN}, {@code SESSION}) são testados em
 * {@link CookiesOverHttpIT}: no MockMvc o spring-security-test troca o repositório de CSRF.
 */
class SecurityConfigIT extends IntegrationTest {

  @Autowired MockMvc mvc;

  @Test
  void publicPathsDoNotRequireLogin() throws Exception {
    mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    mvc.perform(get("/trips/42")).andExpect(forwardedUrl("/index.html"));
  }

  @Test
  void loginPageBelongsToSpa() throws Exception {
    mvc.perform(get("/login")).andExpect(status().isOk()).andExpect(forwardedUrl("/index.html"));
  }

  @Test
  void googleAuthorizationRedirectsToGoogle() throws Exception {
    mvc.perform(get("/oauth2/authorization/google"))
        .andExpect(status().is3xxRedirection())
        .andExpect(
            header().string(HttpHeaders.LOCATION, startsWith("https://accounts.google.com/")))
        .andExpect(header().string(HttpHeaders.LOCATION, containsString("scope=openid")));
  }

  @Test
  void sendsSecurityHeaders() throws Exception {
    mvc.perform(get("/actuator/health"))
        .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
        .andExpect(header().string("X-Content-Type-Options", "nosniff"))
        .andExpect(header().exists("X-Frame-Options"));
  }
}
