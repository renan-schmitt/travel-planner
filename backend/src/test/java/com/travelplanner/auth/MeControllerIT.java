package com.travelplanner.auth;

import static com.travelplanner.auth.TestUsers.loggedIn;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.travelplanner.IntegrationTest;
import com.travelplanner.auth.domain.AppUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

class MeControllerIT extends IntegrationTest {

  @Autowired MockMvc mvc;

  @Autowired AppUserService users;

  @Test
  void withoutSessionReturns401() throws Exception {
    mvc.perform(get("/api/v1/me")).andExpect(status().isUnauthorized());
  }

  @Test
  void returnsLoggedUser() throws Exception {
    var google = TestUsers.googleUser("carla@example.com", "Carla");

    mvc.perform(get("/api/v1/me").with(loggedIn(users, google)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.email").value("carla@example.com"))
        .andExpect(jsonPath("$.name").value("Carla"))
        .andExpect(jsonPath("$.pictureUrl").value(google.getPicture()))
        .andExpect(jsonPath("$.nationality").value("BR"))
        .andExpect(jsonPath("$.homeCity").doesNotExist())
        .andExpect(jsonPath("$.isAdmin").value(false))
        .andExpect(jsonPath("$.version").value(0));
  }

  @Test
  void adminEmailIsCaseInsensitive() throws Exception {
    mvc.perform(get("/api/v1/me").with(loggedIn(users, "Admin@Example.com")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.isAdmin").value(true));
  }

  @Test
  void patchWithoutCsrfReturns403() throws Exception {
    mvc.perform(
            patch("/api/v1/me")
                .with(loggedIn(users, "sem-csrf@example.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\": 0, \"nationality\": \"PT\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void patchUpdatesNationalityAndHomeCity() throws Exception {
    var session = loggedIn(users, "dani@example.com");

    mvc.perform(
            patch("/api/v1/me")
                .with(session)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"version": 0, "nationality": "PT",
                     "homeCity": {"name": " Porto Alegre ", "countryCode": "BR",
                                  "timezone": "America/Sao_Paulo"}}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nationality").value("PT"))
        .andExpect(jsonPath("$.homeCity.name").value("Porto Alegre"))
        .andExpect(jsonPath("$.homeCity.countryCode").value("BR"))
        .andExpect(jsonPath("$.homeCity.timezone").value("America/Sao_Paulo"))
        .andExpect(jsonPath("$.homeCity.placeId").doesNotExist())
        .andExpect(jsonPath("$.version").value(1));

    mvc.perform(get("/api/v1/me").with(session))
        .andExpect(jsonPath("$.homeCity.name").value("Porto Alegre"));

    // homeCity ausente limpa a cidade.
    mvc.perform(
            patch("/api/v1/me")
                .with(session)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\": 1, \"nationality\": \"BR\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.homeCity").doesNotExist());
  }

  @Test
  void patchWithStaleVersionReturns409() throws Exception {
    mvc.perform(
            patch("/api/v1/me")
                .with(loggedIn(users, "edu@example.com"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\": 7, \"nationality\": \"BR\"}"))
        .andExpect(status().isConflict());
  }

  @Test
  void patchValidatesFields() throws Exception {
    mvc.perform(
            patch("/api/v1/me")
                .with(loggedIn(users, "fabi@example.com"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"version": 0, "nationality": "brasil",
                     "homeCity": {"name": "", "countryCode": "bra", "timezone": "Lua/Base"}}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[?(@.field == 'nationality')]").exists())
        .andExpect(jsonPath("$.errors[?(@.field == 'homeCity.name')]").exists())
        .andExpect(jsonPath("$.errors[?(@.field == 'homeCity.countryCode')]").exists())
        .andExpect(jsonPath("$.errors[?(@.field == 'homeCity.timezone')]").exists());
  }
}
