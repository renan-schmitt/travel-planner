package com.travelplanner.itinerary.api;

import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.tomakehurst.wiremock.WireMockServer;
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
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Busca de hotéis no Places (WireMock), proxy de fotos e "adicionar à viagem". */
class LodgingSearchIT extends IntegrationTest {

  static final String KEY = "chave-secreta-de-teste";

  static final WireMockServer PLACES = new WireMockServer(wireMockConfig().dynamicPort());

  static {
    PLACES.start();
  }

  @DynamicPropertySource
  static void placesProperties(DynamicPropertyRegistry registry) {
    registry.add("app.places.base-url", () -> PLACES.baseUrl() + "/v1");
    registry.add("app.places.api-key", () -> KEY);
  }

  @AfterAll
  static void stop() {
    PLACES.resetAll();
  }

  /** Três resultados: com priceLevel, resort sem priceLevel (estimado) e hotel sem faixa. */
  static final String SEARCH_RESPONSE =
      """
      {"places": [
        {"id": "ChIJBarcelo", "displayName": {"text": "Barceló Bávaro Palace"},
         "formattedAddress": "Bávaro, Punta Cana", "location": {"latitude": 18.68, "longitude": -68.42},
         "rating": 4.6, "userRatingCount": 12000, "priceLevel": "PRICE_LEVEL_EXPENSIVE",
         "primaryType": "resort_hotel",
         "photos": [{"name": "places/ChIJBarcelo/photos/AbC_1"}, {"name": "places/ChIJBarcelo/photos/AbC_2"}],
         "websiteUri": "https://barcelo.example", "googleMapsUri": "https://maps.google.com/?cid=1"},
        {"id": "ChIJParadisus", "displayName": {"text": "Paradisus Palma Real"},
         "formattedAddress": "Playa Bávaro", "rating": 4.4, "userRatingCount": 8000,
         "primaryType": "resort_hotel"},
        {"id": "ChIJHotel", "displayName": {"text": "Hotel Centro"}, "primaryType": "hotel"}
      ],
      "nextPageToken": "pagina-2"}
      """;

  @Autowired MockMvc mvc;
  @Autowired AppUserService users;
  @Autowired TripRepository trips;
  @Autowired TripMemberRepository members;
  @Autowired CacheManager caches;

  private record User(long id, RequestPostProcessor session) {}

  @BeforeEach
  void setUp() {
    PLACES.resetAll();
    caches.getCacheNames().forEach(name -> caches.getCache(name).clear());
  }

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

  private static long id(ResultActions result) throws Exception {
    String body = result.andReturn().getResponse().getContentAsString();
    return ((Number) JsonPath.read(body, "$.id")).longValue();
  }

  private record Trip(long id, long stopId) {}

  private Trip puntaCana(User user) throws Exception {
    long tripId =
        id(
            send(
                    user,
                    MockMvcRequestBuilders.post("/api/v1/trips"),
                    "{\"name\": \"Caribe\", \"status\": \"PLANNING\"}")
                .andExpect(status().isCreated()));
    long stopId =
        id(
            send(
                    user,
                    MockMvcRequestBuilders.post("/api/v1/trips/{t}/stops", tripId),
                    """
                    {"cityPlaceId": "ChIJPuntaCana", "cityName": "Punta Cana", "countryCode": "DO",
                     "lat": 18.582, "lng": -68.405, "timezone": "America/Santo_Domingo",
                     "arrivalDate": "2027-01-10", "departureDate": "2027-01-17"}
                    """)
                .andExpect(status().isCreated()));
    return new Trip(tripId, stopId);
  }

  private ResultActions search(User user, Trip trip, String... params) throws Exception {
    var request =
        MockMvcRequestBuilders.get(
                "/api/v1/trips/{t}/stops/{s}/lodging-search", trip.id(), trip.stopId())
            .with(user.session());
    for (int i = 0; i < params.length; i += 2) {
      request.param(params[i], params[i + 1]);
    }
    return mvc.perform(request);
  }

  private void stubSearch() {
    PLACES.stubFor(
        post(urlPathEqualTo("/v1/places:searchText")).willReturn(okJson(SEARCH_RESPONSE)));
  }

  @Test
  void searchSendsFieldMaskAndMapsTiers() throws Exception {
    User ana = newUser();
    Trip trip = puntaCana(ana);
    stubSearch();

    search(ana, trip, "type", "RESORT", "minRating", "4.5")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nextPageToken").value("pagina-2"))
        .andExpect(jsonPath("$.results.length()").value(3))
        .andExpect(jsonPath("$.results[0].placeId").value("ChIJBarcelo"))
        .andExpect(jsonPath("$.results[0].name").value("Barceló Bávaro Palace"))
        .andExpect(jsonPath("$.results[0].rating").value(4.6))
        .andExpect(jsonPath("$.results[0].userRatingCount").value(12000))
        .andExpect(jsonPath("$.results[0].priceTier").value(3))
        .andExpect(jsonPath("$.results[0].priceTierSource").value("PLACES"))
        .andExpect(jsonPath("$.results[0].photoName").value("places/ChIJBarcelo/photos/AbC_1"))
        .andExpect(jsonPath("$.results[0].lat").value(18.68))
        .andExpect(jsonPath("$.results[0].googleMapsUri").value("https://maps.google.com/?cid=1"))
        .andExpect(jsonPath("$.results[1].priceTier").value(3))
        .andExpect(jsonPath("$.results[1].priceTierSource").value("HEURISTIC"))
        .andExpect(jsonPath("$.results[1].photoName").doesNotExist())
        .andExpect(jsonPath("$.results[2].priceTier").doesNotExist());

    PLACES.verify(
        postRequestedFor(urlPathEqualTo("/v1/places:searchText"))
            .withHeader("X-Goog-Api-Key", equalTo(KEY))
            .withHeader(
                "X-Goog-FieldMask",
                equalTo(
                    "places.id,places.displayName,places.formattedAddress,places.location,"
                        + "places.rating,places.userRatingCount,places.priceLevel,"
                        + "places.primaryType,places.photos,places.websiteUri,"
                        + "places.googleMapsUri,nextPageToken"))
            .withRequestBody(
                equalToJson(
                    """
                    {"textQuery": "resorts in Punta Cana, Dominican Republic",
                     "includedType": "lodging",
                     "locationBias": {"circle": {"center": {"latitude": 18.582, "longitude": -68.405},
                                                 "radius": 15000.0}},
                     "languageCode": "pt-BR", "pageSize": 20, "minRating": 4.5}
                    """)));
  }

  @Test
  void filtersByTierIncludingEstimatesAndCachesThePage() throws Exception {
    User ana = newUser();
    Trip trip = puntaCana(ana);
    stubSearch();

    search(ana, trip, "tiers", "3")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.results.length()").value(2))
        .andExpect(jsonPath("$.results[1].placeId").value("ChIJParadisus"));
    // Outra faixa usa a mesma página do cache (o filtro é aplicado depois).
    search(ana, trip, "tiers", "1,2").andExpect(jsonPath("$.results.length()").value(0));
    search(ana, trip).andExpect(jsonPath("$.results.length()").value(3));
    PLACES.verify(1, postRequestedFor(urlPathEqualTo("/v1/places:searchText")));

    // Próxima página: o token vai para o Google e a busca é outra entrada do cache.
    search(ana, trip, "page", "pagina-2").andExpect(status().isOk());
    PLACES.verify(
        postRequestedFor(urlPathEqualTo("/v1/places:searchText"))
            .withRequestBody(matchingJsonPath("$.pageToken", equalTo("pagina-2")))
            .withRequestBody(
                matchingJsonPath(
                    "$.textQuery", equalTo("hotels in Punta Cana, Dominican Republic"))));
    PLACES.verify(2, postRequestedFor(urlPathEqualTo("/v1/places:searchText")));
  }

  @Test
  void addResultAsLodgingKeepsTierSourceAndFetchesNameAndAddress() throws Exception {
    User ana = newUser();
    Trip trip = puntaCana(ana);
    stubSearch();
    PLACES.stubFor(
        get(urlPathEqualTo("/v1/places/ChIJParadisus"))
            .willReturn(
                okJson(
                    """
                    {"id": "ChIJParadisus", "displayName": {"text": "Paradisus Palma Real"},
                     "formattedAddress": "Playa Bávaro, Punta Cana 23000"}
                    """)));
    search(ana, trip, "type", "RESORT").andExpect(status().isOk());

    String lodgings = "/api/v1/trips/{t}/stops/{s}/lodgings";
    send(
            ana,
            MockMvcRequestBuilders.post(lodgings, trip.id(), trip.stopId()),
            """
            {"kind": "HOTEL", "placeId": "ChIJParadisus", "checkIn": "2027-01-10",
             "checkOut": "2027-01-17", "priceTier": 3, "boardType": "ALL_INCLUSIVE"}
            """)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.placeId").value("ChIJParadisus"))
        .andExpect(jsonPath("$.name").value("Paradisus Palma Real"))
        .andExpect(jsonPath("$.address").value("Playa Bávaro, Punta Cana 23000"))
        .andExpect(jsonPath("$.nights").value(7))
        .andExpect(jsonPath("$.priceTier").value(3))
        .andExpect(jsonPath("$.priceTierSource").value("HEURISTIC"));
    PLACES.verify(
        getRequestedFor(urlPathEqualTo("/v1/places/ChIJParadisus"))
            .withHeader("X-Goog-FieldMask", equalTo("id,displayName,formattedAddress")));

    // Faixa diferente da mostrada na busca → o usuário escolheu (USER); o nome dele prevalece.
    send(
            ana,
            MockMvcRequestBuilders.post(lodgings, trip.id(), trip.stopId()),
            """
            {"kind": "HOTEL", "placeId": "ChIJParadisus", "name": "Paradisus (2ª reserva)",
             "checkIn": "2027-01-10", "checkOut": "2027-01-12", "priceTier": 4}
            """)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("Paradisus (2ª reserva)"))
        .andExpect(jsonPath("$.address").value("Playa Bávaro, Punta Cana 23000"))
        .andExpect(jsonPath("$.priceTierSource").value("USER"));
    // Detalhes do lugar em cache.
    PLACES.verify(1, getRequestedFor(urlPathEqualTo("/v1/places/ChIJParadisus")));

    // Lugar inexistente → 400 no placeId; sem placeId, o nome é obrigatório.
    PLACES.stubFor(
        get(urlPathEqualTo("/v1/places/ChIJNada"))
            .willReturn(okJson("{\"error\": {}}").withStatus(404)));
    send(
            ana,
            MockMvcRequestBuilders.post(lodgings, trip.id(), trip.stopId()),
            """
            {"kind": "HOTEL", "placeId": "ChIJNada", "checkIn": "2027-01-10", "checkOut": "2027-01-12"}
            """)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("placeId"));
    send(
            ana,
            MockMvcRequestBuilders.post(lodgings, trip.id(), trip.stopId()),
            """
            {"kind": "HOTEL", "checkIn": "2027-01-10", "checkOut": "2027-01-12"}
            """)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("name"));
  }

  @Test
  void addResultWithPlacesDownUsesTheGivenName() throws Exception {
    User ana = newUser();
    Trip trip = puntaCana(ana);
    PLACES.stubFor(get(urlPathEqualTo("/v1/places/ChIJBarcelo")).willReturn(serverError()));
    String lodgings = "/api/v1/trips/{t}/stops/{s}/lodgings";

    send(
            ana,
            MockMvcRequestBuilders.post(lodgings, trip.id(), trip.stopId()),
            """
            {"kind": "HOTEL", "placeId": "ChIJBarcelo", "name": "Barceló", "checkIn": "2027-01-10",
             "checkOut": "2027-01-12", "priceTier": 3}
            """)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.placeId").value("ChIJBarcelo"))
        .andExpect(jsonPath("$.name").value("Barceló"))
        // Sem busca recente para esse lugar: a faixa é do usuário.
        .andExpect(jsonPath("$.priceTierSource").value("USER"));
    send(
            ana,
            MockMvcRequestBuilders.post(lodgings, trip.id(), trip.stopId()),
            """
            {"kind": "HOTEL", "placeId": "ChIJBarcelo", "checkIn": "2027-01-12", "checkOut": "2027-01-14"}
            """)
        .andExpect(status().isServiceUnavailable());
  }

  @Test
  void photoProxyRedirectsWithoutLeakingTheKey() throws Exception {
    User ana = newUser();
    PLACES.stubFor(
        get(urlPathEqualTo("/v1/places/ChIJBarcelo/photos/AbC_1/media"))
            .willReturn(
                okJson(
                    """
                    {"name": "places/ChIJBarcelo/photos/AbC_1/media",
                     "photoUri": "https://lh3.googleusercontent.com/foto-1"}
                    """)));

    for (int i = 0; i < 2; i++) {
      MvcResult result =
          mvc.perform(
                  MockMvcRequestBuilders.get("/api/v1/places/photos")
                      .param("name", "places/ChIJBarcelo/photos/AbC_1")
                      .param("w", "400")
                      .with(ana.session()))
              .andExpect(status().isFound())
              .andExpect(header().string("Location", "https://lh3.googleusercontent.com/foto-1"))
              .andExpect(header().string("Cache-Control", "max-age=3600, private"))
              .andReturn();
      assertThat(result.getResponse().getContentAsString()).doesNotContain(KEY);
      assertThat(result.getResponse().getHeaderNames())
          .allSatisfy(name -> assertThat(result.getResponse().getHeader(name)).doesNotContain(KEY));
    }
    PLACES.verify(
        1,
        getRequestedFor(urlPathEqualTo("/v1/places/ChIJBarcelo/photos/AbC_1/media"))
            .withQueryParam("maxWidthPx", equalTo("400"))
            .withQueryParam("skipHttpRedirect", equalTo("true"))
            .withQueryParam("key", absent())
            .withHeader("X-Goog-Api-Key", equalTo(KEY)));

    // Nome fora do formato (ex.: tentar outro caminho da API) → 400, sem chamar o Google.
    mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/places/photos")
                .param("name", "places/ChIJBarcelo/../../places:searchText")
                .with(ana.session()))
        .andExpect(status().isBadRequest());
    mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/places/photos")
                .param("name", "places/ChIJBarcelo/photos/AbC_1")
                .param("w", "5000")
                .with(ana.session()))
        .andExpect(status().isBadRequest());
    PLACES.verify(1, getRequestedFor(urlPathEqualTo("/v1/places/ChIJBarcelo/photos/AbC_1/media")));
  }

  @Test
  void searchByRoleAndFailures() throws Exception {
    User ana = newUser();
    User viewer = newUser();
    User stranger = newUser();
    Trip trip = puntaCana(ana);
    members.save(
        TripMember.of(
            trips.findById(trip.id()).orElseThrow(), viewer.id(), TripRole.VIEWER, Instant.now()));
    stubSearch();

    search(viewer, trip).andExpect(status().isOk());
    search(stranger, trip).andExpect(status().isNotFound());
    search(ana, new Trip(trip.id(), 999_999)).andExpect(status().isNotFound());
    search(ana, trip, "tiers", "5").andExpect(status().isBadRequest());
    search(ana, trip, "type", "castle").andExpect(status().isBadRequest());

    PLACES.resetAll();
    PLACES.stubFor(post(urlPathEqualTo("/v1/places:searchText")).willReturn(serverError()));
    search(ana, trip, "minRating", "4").andExpect(status().isServiceUnavailable());
  }
}
