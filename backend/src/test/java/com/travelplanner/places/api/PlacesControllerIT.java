package com.travelplanner.places.api;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.travelplanner.IntegrationTest;
import com.travelplanner.auth.TestUsers;
import com.travelplanner.auth.domain.AppUserService;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Busca de cidades contra um Places falso (WireMock). */
class PlacesControllerIT extends IntegrationTest {

  static final WireMockServer PLACES = new WireMockServer(wireMockConfig().dynamicPort());

  static {
    PLACES.start();
  }

  @DynamicPropertySource
  static void placesProperties(DynamicPropertyRegistry registry) {
    registry.add("app.places.base-url", () -> PLACES.baseUrl() + "/v1");
    registry.add("app.places.api-key", () -> "chave-de-teste");
  }

  @AfterAll
  static void stop() {
    PLACES.resetAll();
  }

  @Autowired MockMvc mvc;
  @Autowired AppUserService users;

  RequestPostProcessor session;

  @BeforeEach
  void setUp() {
    PLACES.resetAll();
    session = TestUsers.loggedIn(users, UUID.randomUUID() + "@example.com");
  }

  @Test
  void autocompleteSendsFieldMaskAndCityTypes() throws Exception {
    PLACES.stubFor(
        post(urlPathEqualTo("/v1/places:autocomplete"))
            .willReturn(
                okJson(
                    """
                    {"suggestions": [
                      {"placePrediction": {"placeId": "ChIJLisboa", "text": {"text": "Lisboa, Portugal"},
                        "structuredFormat": {"mainText": {"text": "Lisboa"},
                                             "secondaryText": {"text": "Portugal"}}}},
                      {"queryPrediction": {"text": {"text": "lisboa hotéis"}}}
                    ]}
                    """)));

    mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/places/cities/autocomplete")
                .param("q", "Lisb")
                .param("session", "sessao-1")
                .with(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].placeId").value("ChIJLisboa"))
        .andExpect(jsonPath("$[0].name").value("Lisboa"))
        .andExpect(jsonPath("$[0].description").value("Portugal"));

    PLACES.verify(
        postRequestedFor(urlPathEqualTo("/v1/places:autocomplete"))
            .withHeader("X-Goog-Api-Key", equalTo("chave-de-teste"))
            .withHeader(
                "X-Goog-FieldMask",
                equalTo(
                    "suggestions.placePrediction.placeId,suggestions.placePrediction.text,"
                        + "suggestions.placePrediction.structuredFormat"))
            .withRequestBody(
                equalToJson(
                    """
                    {"input": "Lisb", "includedPrimaryTypes": ["locality", "administrative_area_level_3"],
                     "languageCode": "pt-BR", "sessionToken": "sessao-1"}
                    """)));
  }

  @Test
  void autocompleteIsCachedByNormalizedInput() throws Exception {
    PLACES.stubFor(
        post(urlPathEqualTo("/v1/places:autocomplete"))
            .willReturn(okJson("{\"suggestions\": []}")));

    for (String q : new String[] {"Porto Alegre", "  porto   ALEGRE "}) {
      mvc.perform(
              MockMvcRequestBuilders.get("/api/v1/places/cities/autocomplete")
                  .param("q", q)
                  .with(session))
          .andExpect(status().isOk());
    }

    PLACES.verify(1, postRequestedFor(urlPathEqualTo("/v1/places:autocomplete")));
  }

  @Test
  void cityDetailsMapCountryAndTimezone() throws Exception {
    PLACES.stubFor(
        get(urlPathEqualTo("/v1/places/ChIJPorto"))
            .willReturn(
                okJson(
                    """
                    {"id": "ChIJPorto", "displayName": {"text": "Porto", "languageCode": "pt"},
                     "formattedAddress": "Porto, Portugal",
                     "location": {"latitude": 41.1579438, "longitude": -8.6291053},
                     "addressComponents": [
                       {"longText": "Porto", "shortText": "Porto", "types": ["locality", "political"]},
                       {"longText": "Portugal", "shortText": "PT", "types": ["country", "political"]}
                     ]}
                    """)));

    mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/places/cities/ChIJPorto")
                .param("session", "sessao-2")
                .with(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.placeId").value("ChIJPorto"))
        .andExpect(jsonPath("$.name").value("Porto"))
        .andExpect(jsonPath("$.countryCode").value("PT"))
        .andExpect(jsonPath("$.lat").value(41.1579438))
        .andExpect(jsonPath("$.lng").value(-8.6291053))
        .andExpect(jsonPath("$.timezone").value("Europe/Lisbon"));

    PLACES.verify(
        getRequestedFor(urlPathEqualTo("/v1/places/ChIJPorto"))
            .withHeader(
                "X-Goog-FieldMask",
                equalTo("id,displayName,formattedAddress,location,addressComponents"))
            .withQueryParam("sessionToken", equalTo("sessao-2"))
            .withQueryParam("languageCode", equalTo("pt-BR")));
  }

  @Test
  void timezoneComesFromCoordinatesAcrossTheWorld() throws Exception {
    PLACES.stubFor(
        get(urlPathEqualTo("/v1/places/ChIJManaus"))
            .willReturn(
                okJson(
                    """
                    {"id": "ChIJManaus", "displayName": {"text": "Manaus"},
                     "location": {"latitude": -3.1190275, "longitude": -60.0217314},
                     "addressComponents": [{"shortText": "BR", "types": ["country"]}]}
                    """)));

    mvc.perform(MockMvcRequestBuilders.get("/api/v1/places/cities/ChIJManaus").with(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.countryCode").value("BR"))
        .andExpect(jsonPath("$.timezone").value("America/Manaus"));
  }

  @Test
  void quotaExhaustedBecomes503() throws Exception {
    PLACES.stubFor(
        post(urlPathEqualTo("/v1/places:autocomplete"))
            .willReturn(
                aResponse()
                    .withStatus(429)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"error\": {\"code\": 429, \"status\": \"RESOURCE_EXHAUSTED\"}}")));

    mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/places/cities/autocomplete")
                .param("q", "Quota")
                .with(session))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.detail").value("Busca de lugares indisponível."));

    // A falha não fica no cache: quando o Places voltar, a mesma busca funciona.
    PLACES.stubFor(
        post(urlPathEqualTo("/v1/places:autocomplete"))
            .willReturn(okJson("{\"suggestions\": []}")));
    mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/places/cities/autocomplete")
                .param("q", "Quota")
                .with(session))
        .andExpect(status().isOk());
  }

  @Test
  void unknownPlaceIs404() throws Exception {
    PLACES.stubFor(
        get(urlPathEqualTo("/v1/places/ChIJNada"))
            .willReturn(
                aResponse()
                    .withStatus(404)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"error\": {\"code\": 404, \"status\": \"NOT_FOUND\"}}")));

    mvc.perform(MockMvcRequestBuilders.get("/api/v1/places/cities/ChIJNada").with(session))
        .andExpect(status().isNotFound());
  }

  @Test
  void shortQueryIs400AndAnonymousIs401() throws Exception {
    mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/places/cities/autocomplete")
                .param("q", "Li")
                .with(session))
        .andExpect(status().isBadRequest());
    mvc.perform(MockMvcRequestBuilders.get("/api/v1/places/cities/autocomplete").param("q", "Lis"))
        .andExpect(status().isUnauthorized());
    PLACES.verify(0, postRequestedFor(urlPathEqualTo("/v1/places:autocomplete")));
  }
}
