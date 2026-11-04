package com.travelplanner.itinerary.api;

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
import java.util.List;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Busca de restaurantes/atrações (WireMock), favoritos e as regras {@code RESTAURANTES}, {@code
 * ATRACOES} e {@code RESORT_OPCOES}.
 */
class SavedPlaceIT extends IntegrationTest {

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

  /** Com priceLevel, fast-food sem priceLevel (estimado) e sem faixa/nota. */
  static final String RESTAURANTS =
      """
      {"places": [
        {"id": "ChIJBelcanto", "displayName": {"text": "Belcanto"},
         "formattedAddress": "Chiado, Lisboa", "location": {"latitude": 38.71, "longitude": -9.14},
         "rating": 4.7, "userRatingCount": 3000, "priceLevel": "PRICE_LEVEL_VERY_EXPENSIVE",
         "primaryType": "fine_dining_restaurant", "photos": [{"name": "places/ChIJBelcanto/photos/F1"}],
         "googleMapsUri": "https://maps.google.com/?cid=10"},
        {"id": "ChIJBifana", "displayName": {"text": "Bifanas do Afonso"}, "rating": 4.5,
         "userRatingCount": 900, "primaryType": "fast_food_restaurant"},
        {"id": "ChIJTasca", "displayName": {"text": "Tasca"}, "primaryType": "restaurant"}
      ]}
      """;

  static final String ATTRACTIONS =
      """
      {"places": [
        {"id": "ChIJJeronimos", "displayName": {"text": "Mosteiro dos Jerónimos"}, "rating": 4.7,
         "primaryType": "historical_landmark"},
        {"id": "ChIJOceanario", "displayName": {"text": "Oceanário"}, "rating": 4.6,
         "primaryType": "aquarium"}
      ]}
      """;

  @Autowired MockMvc mvc;
  @Autowired AppUserService users;
  @Autowired TripRepository trips;
  @Autowired TripMemberRepository members;
  @Autowired CacheManager caches;

  private record User(long id, RequestPostProcessor session) {}

  private record Trip(long id, long stopId) {}

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

  private ResultActions fetch(User user, String url, Object... vars) throws Exception {
    return mvc.perform(MockMvcRequestBuilders.get(url, vars).with(user.session()));
  }

  private static long id(ResultActions result) throws Exception {
    return ((Number) JsonPath.read(body(result), "$.id")).longValue();
  }

  private static String body(ResultActions result) throws Exception {
    return result.andReturn().getResponse().getContentAsString();
  }

  /** Lisboa, 3 noites (com coordenadas, se {@code withCoordinates}). */
  private Trip lisboa(User user, boolean withCoordinates) throws Exception {
    long tripId =
        id(
            send(
                    user,
                    MockMvcRequestBuilders.post("/api/v1/trips"),
                    "{\"name\": \"Portugal\", \"status\": \"PLANNING\"}")
                .andExpect(status().isCreated()));
    String coordinates = withCoordinates ? "\"lat\": 38.7223, \"lng\": -9.1393," : "";
    long stopId =
        id(
            send(
                    user,
                    MockMvcRequestBuilders.post("/api/v1/trips/{t}/stops", tripId),
                    """
                    {"cityName": "Lisboa", "countryCode": "PT", %s
                     "timezone": "Europe/Lisbon", "arrivalDate": "2027-03-11",
                     "departureDate": "2027-03-14"}
                    """
                        .formatted(coordinates))
                .andExpect(status().isCreated()));
    return new Trip(tripId, stopId);
  }

  private ResultActions search(User user, Trip trip, String kind, String... params)
      throws Exception {
    var request =
        MockMvcRequestBuilders.get(
                "/api/v1/trips/{t}/stops/{s}/places-search", trip.id(), trip.stopId())
            .param("kind", kind)
            .with(user.session());
    for (int i = 0; i < params.length; i += 2) {
      request.param(params[i], params[i + 1]);
    }
    return mvc.perform(request);
  }

  private void stubNearby() {
    PLACES.stubFor(
        post(urlPathEqualTo("/v1/places:searchNearby"))
            .withRequestBody(matchingJsonPath("$.includedTypes[0]", equalTo("restaurant")))
            .willReturn(okJson(RESTAURANTS)));
    PLACES.stubFor(
        post(urlPathEqualTo("/v1/places:searchNearby"))
            .withRequestBody(matchingJsonPath("$.includedTypes[0]", equalTo("tourist_attraction")))
            .willReturn(okJson(ATTRACTIONS)));
  }

  private ResultActions save(User user, Trip trip, String placeId, String kind, String extra)
      throws Exception {
    return send(
        user,
        MockMvcRequestBuilders.post(
            "/api/v1/trips/{t}/stops/{s}/saved-places", trip.id(), trip.stopId()),
        "{\"placeId\": \"%s\", \"kind\": \"%s\"%s}"
            .formatted(placeId, kind, extra.isEmpty() ? "" : ", " + extra));
  }

  private List<String> pendingRules(User user, Trip trip) throws Exception {
    return JsonPath.read(
        body(
            fetch(user, "/api/v1/trips/{t}/suggestions?status=PENDING", trip.id())
                .andExpect(status().isOk())),
        "$[*].ruleCode");
  }

  @Test
  void nearbySearchFromTheCitySendsFieldMaskAndFilters() throws Exception {
    User ana = newUser();
    Trip trip = lisboa(ana, true);
    stubNearby();

    search(ana, trip, "RESTAURANT")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.center").value("CITY"))
        .andExpect(jsonPath("$.centerLat").value(38.7223))
        .andExpect(jsonPath("$.results.length()").value(3))
        .andExpect(jsonPath("$.results[0].placeId").value("ChIJBelcanto"))
        .andExpect(jsonPath("$.results[0].priceTier").value(4))
        .andExpect(jsonPath("$.results[0].priceTierSource").value("PLACES"))
        .andExpect(jsonPath("$.results[0].photoName").value("places/ChIJBelcanto/photos/F1"))
        .andExpect(jsonPath("$.results[0].googleMapsUri").value("https://maps.google.com/?cid=10"))
        .andExpect(jsonPath("$.results[1].priceTier").value(1))
        .andExpect(jsonPath("$.results[1].priceTierSource").value("HEURISTIC"))
        .andExpect(jsonPath("$.results[2].priceTier").doesNotExist());

    PLACES.verify(
        postRequestedFor(urlPathEqualTo("/v1/places:searchNearby"))
            .withHeader("X-Goog-Api-Key", equalTo(KEY))
            .withHeader(
                "X-Goog-FieldMask",
                equalTo(
                    "places.id,places.displayName,places.formattedAddress,places.location,"
                        + "places.rating,places.userRatingCount,places.priceLevel,"
                        + "places.primaryType,places.photos,places.googleMapsUri"))
            .withRequestBody(
                equalToJson(
                    """
                    {"includedTypes": ["restaurant"], "maxResultCount": 20,
                     "locationRestriction": {"circle": {"center": {"latitude": 38.7223,
                       "longitude": -9.1393}, "radius": 5000.0}},
                     "rankPreference": "POPULARITY", "languageCode": "pt-BR"}
                    """)));

    // Nota e faixa filtradas aqui (o Nearby não filtra), sobre a mesma resposta em cache.
    search(ana, trip, "RESTAURANT", "minRating", "4.6")
        .andExpect(jsonPath("$.results.length()").value(1))
        .andExpect(jsonPath("$.results[0].placeId").value("ChIJBelcanto"));
    search(ana, trip, "RESTAURANT", "tiers", "1,2")
        .andExpect(jsonPath("$.results.length()").value(1))
        .andExpect(jsonPath("$.results[0].placeId").value("ChIJBifana"));
    PLACES.verify(1, postRequestedFor(urlPathEqualTo("/v1/places:searchNearby")));

    search(ana, trip, "ATTRACTION")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.results.length()").value(2));
    PLACES.verify(
        postRequestedFor(urlPathEqualTo("/v1/places:searchNearby"))
            .withRequestBody(
                matchingJsonPath(
                    "$.includedTypes",
                    equalToJson(
                        "[\"tourist_attraction\", \"museum\", \"park\", \"beach\","
                            + " \"historical_landmark\"]")))
            .withRequestBody(
                matchingJsonPath("$.locationRestriction.circle.radius", equalTo("10000.0"))));
  }

  @Test
  void searchCentersOnTheLodgingOrFallsBackToText() throws Exception {
    User ana = newUser();
    Trip trip = lisboa(ana, true);
    stubNearby();
    PLACES.stubFor(
        get(urlPathEqualTo("/v1/places/ChIJHotelAvenida"))
            .withHeader(
                "X-Goog-FieldMask", equalTo("id,location,rating,userRatingCount,googleMapsUri"))
            .willReturn(
                okJson(
                    """
                    {"id": "ChIJHotelAvenida", "location": {"latitude": 38.72, "longitude": -9.145}}
                    """)));
    PLACES.stubFor(
        get(urlPathEqualTo("/v1/places/ChIJHotelAvenida"))
            .withHeader("X-Goog-FieldMask", equalTo("id,displayName,formattedAddress"))
            .willReturn(okJson("{\"id\": \"ChIJHotelAvenida\", \"formattedAddress\": \"Av.\"}")));
    send(
            ana,
            MockMvcRequestBuilders.post(
                "/api/v1/trips/{t}/stops/{s}/lodgings", trip.id(), trip.stopId()),
            """
            {"kind": "HOTEL", "placeId": "ChIJHotelAvenida", "name": "Hotel Avenida",
             "checkIn": "2027-03-11", "checkOut": "2027-03-14"}
            """)
        .andExpect(status().isCreated());

    search(ana, trip, "RESTAURANT")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.center").value("LODGING"))
        .andExpect(jsonPath("$.centerLodgingName").value("Hotel Avenida"))
        .andExpect(jsonPath("$.centerLng").value(-9.145));
    PLACES.verify(
        postRequestedFor(urlPathEqualTo("/v1/places:searchNearby"))
            .withRequestBody(
                matchingJsonPath(
                    "$.locationRestriction.circle.center.latitude", equalTo("38.72"))));

    // Parada digitada à mão (sem coordenadas): busca por texto na cidade.
    Trip manual = lisboa(ana, false);
    PLACES.stubFor(post(urlPathEqualTo("/v1/places:searchText")).willReturn(okJson(ATTRACTIONS)));
    search(ana, manual, "ATTRACTION")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.center").value("TEXT"))
        .andExpect(jsonPath("$.results.length()").value(2));
    PLACES.verify(
        postRequestedFor(urlPathEqualTo("/v1/places:searchText"))
            .withRequestBody(
                equalToJson(
                    """
                    {"textQuery": "tourist attractions in Lisboa, Portugal",
                     "languageCode": "pt-BR", "pageSize": 20}
                    """)));
  }

  @Test
  void favoritesResolveTheSuggestions() throws Exception {
    User ana = newUser();
    Trip trip = lisboa(ana, true);
    stubNearby();
    assertThat(pendingRules(ana, trip)).contains("RESTAURANTES", "ATRACOES");

    search(ana, trip, "RESTAURANT").andExpect(status().isOk());
    search(ana, trip, "ATTRACTION").andExpect(status().isOk());
    // Faixa igual à da busca → origem da busca; diferente → USER; sem faixa → nula.
    long belcanto =
        id(
            save(
                    ana,
                    trip,
                    "ChIJBelcanto",
                    "RESTAURANT",
                    "\"name\": \"Belcanto\", \"priceTier\": 4")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Belcanto"))
                .andExpect(jsonPath("$.priceTierSource").value("PLACES"))
                .andExpect(jsonPath("$.visited").value(false)));
    save(ana, trip, "ChIJBifana", "RESTAURANT", "\"name\": \"Bifanas\", \"priceTier\": 1")
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.priceTierSource").value("HEURISTIC"));
    save(ana, trip, "ChIJTasca", "RESTAURANT", "\"name\": \"Tasca\", \"priceTier\": 2")
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.priceTierSource").value("USER"));
    assertThat(pendingRules(ana, trip)).doesNotContain("RESTAURANTES").contains("ATRACOES");

    // Sem nome: vem do Places.
    PLACES.stubFor(
        get(urlPathEqualTo("/v1/places/ChIJJeronimos"))
            .withHeader("X-Goog-FieldMask", equalTo("id,displayName,formattedAddress"))
            .willReturn(
                okJson("{\"id\": \"ChIJJeronimos\", \"displayName\": {\"text\": \"Jerónimos\"}}")));
    save(ana, trip, "ChIJJeronimos", "ATTRACTION", "")
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("Jerónimos"))
        .andExpect(jsonPath("$.priceTier").doesNotExist());
    long oceanario =
        id(
            save(ana, trip, "ChIJOceanario", "ATTRACTION", "\"name\": \"Oceanário\"")
                .andExpect(status().isCreated()));
    assertThat(pendingRules(ana, trip)).doesNotContain("RESTAURANTES", "ATRACOES");

    // O mesmo lugar duas vezes na parada → 409.
    save(ana, trip, "ChIJBelcanto", "RESTAURANT", "\"name\": \"Belcanto\"")
        .andExpect(status().isConflict());

    // Listagem por parada: nota e link do Maps pelo Places (cache de 24 h).
    PLACES.stubFor(
        get(urlPathEqualTo("/v1/places/ChIJBelcanto"))
            .withHeader(
                "X-Goog-FieldMask", equalTo("id,location,rating,userRatingCount,googleMapsUri"))
            .willReturn(
                okJson(
                    """
                    {"id": "ChIJBelcanto", "rating": 4.7, "userRatingCount": 3001,
                     "location": {"latitude": 38.71, "longitude": -9.14},
                     "googleMapsUri": "https://maps.google.com/?cid=10"}
                    """)));
    String stopList = "/api/v1/trips/{t}/stops/{s}/saved-places";
    for (int i = 0; i < 2; i++) {
      fetch(ana, stopList, trip.id(), trip.stopId())
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(5))
          .andExpect(jsonPath("$[0].placeId").value("ChIJBelcanto"))
          .andExpect(jsonPath("$[0].rating").value(4.7))
          .andExpect(jsonPath("$[0].userRatingCount").value(3001))
          .andExpect(jsonPath("$[0].googleMapsUri").value("https://maps.google.com/?cid=10"))
          .andExpect(jsonPath("$[1].rating").doesNotExist());
    }
    PLACES.verify(1, getRequestedFor(urlPathEqualTo("/v1/places/ChIJBelcanto")));
    fetch(ana, "/api/v1/trips/{t}/saved-places", trip.id())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(5))
        .andExpect(jsonPath("$[0].rating").doesNotExist());

    // Editar: visitado, notas e faixa (alterada → USER); versão antiga → 409.
    String item = "/api/v1/trips/{t}/stops/{s}/saved-places/{p}";
    send(
            ana,
            MockMvcRequestBuilders.patch(item, trip.id(), trip.stopId(), belcanto),
            """
            {"version": 0, "kind": "RESTAURANT", "priceTier": 3, "notes": " Reservar ",
             "visited": true}
            """)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.visited").value(true))
        .andExpect(jsonPath("$.notes").value("Reservar"))
        .andExpect(jsonPath("$.priceTierSource").value("USER"))
        .andExpect(jsonPath("$.version").value(1));
    send(
            ana,
            MockMvcRequestBuilders.patch(item, trip.id(), trip.stopId(), belcanto),
            "{\"version\": 0, \"kind\": \"RESTAURANT\", \"visited\": false}")
        .andExpect(status().isConflict());

    // Trocar o tipo da última atração ou excluí-la traz a sugestão de volta.
    mvc.perform(
            MockMvcRequestBuilders.delete(item, trip.id(), trip.stopId(), oceanario)
                .with(ana.session())
                .with(csrf()))
        .andExpect(status().isNoContent());
    assertThat(pendingRules(ana, trip)).doesNotContain("ATRACOES");
    List<Number> ids =
        JsonPath.read(
            body(fetch(ana, "/api/v1/trips/{t}/saved-places", trip.id())),
            "$[?(@.placeId == 'ChIJJeronimos')].id");
    long jeronimos = ids.getFirst().longValue();
    send(
            ana,
            MockMvcRequestBuilders.patch(item, trip.id(), trip.stopId(), jeronimos),
            "{\"version\": 0, \"kind\": \"OTHER\", \"visited\": false}")
        .andExpect(status().isOk());
    assertThat(pendingRules(ana, trip)).contains("ATRACOES");
  }

  @Test
  void listingWorksWithPlacesDown() throws Exception {
    User ana = newUser();
    Trip trip = lisboa(ana, true);
    save(ana, trip, "ChIJBelcanto", "RESTAURANT", "\"name\": \"Belcanto\"")
        .andExpect(status().isCreated());
    save(ana, trip, "ChIJTasca", "RESTAURANT", "\"name\": \"Tasca\"")
        .andExpect(status().isCreated());
    PLACES.stubFor(get(urlPathEqualTo("/v1/places/ChIJBelcanto")).willReturn(serverError()));

    fetch(ana, "/api/v1/trips/{t}/stops/{s}/saved-places", trip.id(), trip.stopId())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].rating").doesNotExist());
    // Desiste depois da primeira falha (não espera o timeout de cada um).
    PLACES.verify(0, getRequestedFor(urlPathEqualTo("/v1/places/ChIJTasca")));

    // Sem nome e com o Places fora do ar → 503; lugar inexistente → 400 no placeId.
    PLACES.stubFor(get(urlPathEqualTo("/v1/places/ChIJSemNome")).willReturn(serverError()));
    save(ana, trip, "ChIJSemNome", "OTHER", "").andExpect(status().isServiceUnavailable());
    PLACES.stubFor(
        get(urlPathEqualTo("/v1/places/ChIJNada"))
            .willReturn(okJson("{\"error\": {}}").withStatus(404)));
    save(ana, trip, "ChIJNada", "OTHER", "")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("placeId"));
  }

  @Test
  void resortLodgingSuggestsItsRestaurants() throws Exception {
    User ana = newUser();
    Trip trip = lisboa(ana, true);
    send(
            ana,
            MockMvcRequestBuilders.post(
                "/api/v1/trips/{t}/stops/{s}/lodgings", trip.id(), trip.stopId()),
            """
            {"kind": "HOTEL", "name": "Lisbon Resort", "boardType": "ALL_INCLUSIVE",
             "checkIn": "2027-03-11", "checkOut": "2027-03-14"}
            """)
        .andExpect(status().isCreated());

    String suggestions = body(fetch(ana, "/api/v1/trips/{t}/suggestions", trip.id()));
    List<String> titles =
        JsonPath.read(suggestions, "$[?(@.ruleCode == 'RESORT_OPCOES')].checklist.title");
    assertThat(titles)
        .containsExactly("Ver restaurantes à la carte/reservas do resort: Lisbon Resort");
    List<String> severities =
        JsonPath.read(suggestions, "$[?(@.ruleCode == 'RESORT_OPCOES')].severity");
    assertThat(severities).containsExactly("INFO");
  }

  @Test
  void accessByRoleAndValidation() throws Exception {
    User ana = newUser();
    User viewer = newUser();
    User stranger = newUser();
    Trip trip = lisboa(ana, true);
    members.save(
        TripMember.of(
            trips.findById(trip.id()).orElseThrow(), viewer.id(), TripRole.VIEWER, Instant.now()));
    stubNearby();
    long saved =
        id(
            save(ana, trip, "ChIJBelcanto", "RESTAURANT", "\"name\": \"Belcanto\"")
                .andExpect(status().isCreated()));
    String list = "/api/v1/trips/{t}/stops/{s}/saved-places";

    search(viewer, trip, "RESTAURANT").andExpect(status().isOk());
    fetch(viewer, list, trip.id(), trip.stopId()).andExpect(status().isOk());
    save(viewer, trip, "ChIJTasca", "RESTAURANT", "\"name\": \"Tasca\"")
        .andExpect(status().isForbidden());
    send(
            viewer,
            MockMvcRequestBuilders.patch(list + "/{p}", trip.id(), trip.stopId(), saved),
            "{\"version\": 0, \"kind\": \"RESTAURANT\", \"visited\": true}")
        .andExpect(status().isForbidden());

    search(stranger, trip, "RESTAURANT").andExpect(status().isNotFound());
    fetch(stranger, list, trip.id(), trip.stopId()).andExpect(status().isNotFound());
    fetch(stranger, "/api/v1/trips/{t}/saved-places", trip.id()).andExpect(status().isNotFound());
    fetch(ana, list, trip.id(), 999_999).andExpect(status().isNotFound());

    search(ana, trip, "castle").andExpect(status().isBadRequest());
    search(ana, trip, "RESTAURANT", "tiers", "0").andExpect(status().isBadRequest());
    save(ana, trip, "../places:searchText", "RESTAURANT", "").andExpect(status().isBadRequest());
    save(ana, trip, "ChIJTasca", "HOTEL", "").andExpect(status().isBadRequest());

    PLACES.resetAll();
    caches.getCacheNames().forEach(name -> caches.getCache(name).clear());
    PLACES.stubFor(post(urlPathEqualTo("/v1/places:searchNearby")).willReturn(serverError()));
    search(ana, trip, "RESTAURANT").andExpect(status().isServiceUnavailable());

    // Excluir a parada exclui os favoritos.
    mvc.perform(
            MockMvcRequestBuilders.delete("/api/v1/trips/{t}/stops/{s}", trip.id(), trip.stopId())
                .with(ana.session())
                .with(csrf()))
        .andExpect(status().is2xxSuccessful());
    fetch(ana, "/api/v1/trips/{t}/saved-places", trip.id())
        .andExpect(jsonPath("$.length()").value(0));
  }
}
