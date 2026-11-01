package com.travelplanner.places.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.travelplanner.IntegrationTest;
import com.travelplanner.auth.TestUsers;
import com.travelplanner.auth.domain.AppUserService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

/** Sem {@code PLACES_API_KEY} (ex.: dev local), a busca responde 503 sem chamar o Google. */
class PlacesWithoutKeyIT extends IntegrationTest {

  @Autowired MockMvc mvc;
  @Autowired AppUserService users;

  @Test
  void missingKeyIs503() throws Exception {
    mvc.perform(
            get("/api/v1/places/cities/autocomplete")
                .param("q", "Lisboa")
                .with(TestUsers.loggedIn(users, UUID.randomUUID() + "@example.com")))
        .andExpect(status().isServiceUnavailable());
  }
}
