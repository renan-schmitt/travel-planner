package com.travelplanner.shared.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.travelplanner.IntegrationTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

/** Usa o {@code static/index.html} e o {@code static/app.js} de {@code src/test/resources}. */
class SpaForwardControllerIT extends IntegrationTest {

  @Autowired MockMvc mvc;

  @ParameterizedTest
  @ValueSource(strings = {"/", "/trips", "/trips/42", "/trips/42/days/3", "/login", "/dev/ui"})
  void forwardsSpaRoutesToIndex(String path) throws Exception {
    mvc.perform(get(path)).andExpect(status().isOk()).andExpect(forwardedUrl("/index.html"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"/actuator/inexistente", "/assets/inexistente.js", "/trips/arquivo.png"})
  void doesNotForwardBackendPathsOrFiles(String path) throws Exception {
    mvc.perform(get(path)).andExpect(status().isNotFound()).andExpect(forwardedUrl(null));
  }

  @ParameterizedTest
  @ValueSource(strings = {"/api", "/api/v1/inexistente"})
  void doesNotForwardApiPaths(String path) throws Exception {
    mvc.perform(get(path).with(user("ana")))
        .andExpect(status().isNotFound())
        .andExpect(forwardedUrl(null));
  }

  /** O fluxo OAuth é tratado pelos filtros do Spring Security (ver {@code SecurityConfigIT}). */
  @ParameterizedTest
  @ValueSource(strings = {"/oauth2/authorization/google", "/login/oauth2/code/google"})
  void doesNotForwardOAuthPaths(String path) throws Exception {
    mvc.perform(get(path)).andExpect(status().is3xxRedirection()).andExpect(forwardedUrl(null));
  }

  @ParameterizedTest
  @ValueSource(strings = {"/app.js", "/index.html"})
  void servesStaticFiles(String path) throws Exception {
    mvc.perform(get(path))
        .andExpect(status().isOk())
        .andExpect(forwardedUrl(null))
        .andExpect(content().string(containsString("de-teste")));
  }
}
