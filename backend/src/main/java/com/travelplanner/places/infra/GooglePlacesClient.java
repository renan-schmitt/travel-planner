package com.travelplanner.places.infra;

import com.travelplanner.places.domain.CitySuggestion;
import com.travelplanner.places.domain.LodgingPage;
import com.travelplanner.places.domain.LodgingQuery;
import com.travelplanner.places.domain.LodgingType;
import com.travelplanner.places.domain.NearbyKind;
import com.travelplanner.places.domain.NearbyQuery;
import com.travelplanner.places.domain.PlaceCity;
import com.travelplanner.places.domain.PlaceInfo;
import com.travelplanner.places.domain.PlaceResult;
import com.travelplanner.places.domain.PlaceSummary;
import com.travelplanner.places.domain.PlacesClient;
import com.travelplanner.places.domain.PriceTierMapper;
import com.travelplanner.shared.web.NotFoundException;
import com.travelplanner.shared.web.ServiceUnavailableException;
import java.net.http.HttpClient;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Cliente da Places API (New). Toda chamada leva {@code X-Goog-FieldMask} com só os campos usados,
 * para cair nos SKUs mais baratos (ESP §12). Sem chave configurada, responde como indisponível sem
 * chamar o Google.
 */
@Slf4j
@Component
class GooglePlacesClient implements PlacesClient {

  static final String AUTOCOMPLETE_FIELD_MASK =
      "suggestions.placePrediction.placeId,suggestions.placePrediction.text,"
          + "suggestions.placePrediction.structuredFormat";

  static final String CITY_FIELD_MASK =
      "id,displayName,formattedAddress,location,addressComponents";

  static final String LODGING_FIELD_MASK =
      "places.id,places.displayName,places.formattedAddress,places.location,places.rating,"
          + "places.userRatingCount,places.priceLevel,places.primaryType,places.photos,"
          + "places.websiteUri,places.googleMapsUri,nextPageToken";

  /** Como a da hospedagem, sem {@code websiteUri} e sem paginação (o Nearby não pagina). */
  static final String NEARBY_FIELD_MASK =
      "places.id,places.displayName,places.formattedAddress,places.location,places.rating,"
          + "places.userRatingCount,places.priceLevel,places.primaryType,places.photos,"
          + "places.googleMapsUri";

  static final String SUMMARY_FIELD_MASK = "id,displayName,formattedAddress";

  static final String INFO_FIELD_MASK = "id,location,rating,userRatingCount,googleMapsUri";

  static final int NEARBY_MAX_RESULTS = 20;

  /** Raio do {@code locationBias} da busca de hospedagem, em metros. */
  static final double LODGING_RADIUS_M = 15_000;

  static final int LODGING_PAGE_SIZE = 20;

  private static final List<String> CITY_TYPES = List.of("locality", "administrative_area_level_3");

  private static final String UNAVAILABLE = "Busca de lugares indisponível.";

  private final RestClient http;
  private final PlacesProperties properties;

  GooglePlacesClient(RestClient.Builder builder, PlacesProperties properties) {
    this.properties = properties;
    JdkClientHttpRequestFactory requestFactory =
        new JdkClientHttpRequestFactory(
            HttpClient.newBuilder().connectTimeout(properties.connectTimeout()).build());
    requestFactory.setReadTimeout(properties.readTimeout());
    this.http =
        builder
            .baseUrl(properties.baseUrl())
            .requestFactory(requestFactory)
            .defaultHeader("X-Goog-Api-Key", Objects.requireNonNullElse(properties.apiKey(), ""))
            .build();
  }

  @Override
  public List<CitySuggestion> autocompleteCities(
      String input, String sessionToken, String languageCode) {
    requireConfigured();
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("input", input);
    body.put("includedPrimaryTypes", CITY_TYPES);
    body.put("languageCode", languageCode);
    if (sessionToken != null) {
      body.put("sessionToken", sessionToken);
    }
    AutocompleteResponse response =
        call(
            "autocomplete",
            () ->
                http.post()
                    .uri("/places:autocomplete")
                    .header("X-Goog-FieldMask", AUTOCOMPLETE_FIELD_MASK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(AutocompleteResponse.class));
    if (response == null || response.suggestions() == null) {
      return List.of();
    }
    return response.suggestions().stream()
        .map(Suggestion::placePrediction)
        .filter(p -> p != null && p.placeId() != null)
        .map(GooglePlacesClient::toSuggestion)
        .toList();
  }

  @Override
  public PlaceCity getCity(String placeId, String sessionToken, String languageCode) {
    requireConfigured();
    PlaceDetails details;
    try {
      details =
          call(
              "details",
              () ->
                  http.get()
                      .uri(
                          uri ->
                              uri.path("/places/{placeId}")
                                  .queryParam("languageCode", languageCode)
                                  .queryParamIfPresent(
                                      "sessionToken", Optional.ofNullable(sessionToken))
                                  .build(placeId))
                      .header("X-Goog-FieldMask", CITY_FIELD_MASK)
                      .retrieve()
                      .body(PlaceDetails.class));
    } catch (PlaceNotFound e) {
      throw new NotFoundException("Cidade não encontrada.");
    }
    if (details == null || details.location() == null) {
      throw new NotFoundException("Cidade não encontrada.");
    }
    String name =
        details.displayName() != null && details.displayName().text() != null
            ? details.displayName().text()
            : details.formattedAddress();
    return new PlaceCity(
        Objects.requireNonNullElse(details.id(), placeId),
        name,
        countryCode(details.addressComponents()),
        details.location().latitude(),
        details.location().longitude());
  }

  @Override
  public LodgingPage searchLodging(LodgingQuery query, String languageCode) {
    requireConfigured();
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("textQuery", lodgingText(query));
    body.put("includedType", "lodging");
    if (query.lat() != null && query.lng() != null) {
      body.put(
          "locationBias",
          Map.of(
              "circle",
              Map.of(
                  "center",
                  Map.of("latitude", query.lat(), "longitude", query.lng()),
                  "radius",
                  LODGING_RADIUS_M)));
    }
    body.put("languageCode", languageCode);
    body.put("pageSize", LODGING_PAGE_SIZE);
    if (query.minRating() != null) {
      body.put("minRating", query.minRating());
    }
    if (query.pageToken() != null) {
      body.put("pageToken", query.pageToken());
    }
    SearchTextResponse response =
        call(
            "searchText",
            () ->
                http.post()
                    .uri("/places:searchText")
                    .header("X-Goog-FieldMask", LODGING_FIELD_MASK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(SearchTextResponse.class));
    if (response == null) {
      return new LodgingPage(List.of(), null);
    }
    List<PlaceResult> places =
        response.places() == null
            ? List.of()
            : response.places().stream()
                .filter(p -> p.id() != null)
                .map(GooglePlacesClient::toResult)
                .toList();
    String next =
        response.nextPageToken() == null || response.nextPageToken().isBlank()
            ? null
            : response.nextPageToken();
    return new LodgingPage(places, next);
  }

  /** "hotels in Lisboa, Portugal" (ou "resorts in …"); o nome do país vai em inglês. */
  static String lodgingText(LodgingQuery query) {
    String noun = query.type() == LodgingType.RESORT ? "resorts" : "hotels";
    return inCity(noun, query.city(), query.countryCode());
  }

  /** "&lt;noun&gt; in Lisboa, Portugal"; o nome do país vai em inglês. */
  static String inCity(String noun, String city, String countryCode) {
    String country =
        countryCode == null ? "" : Locale.of("", countryCode).getDisplayCountry(Locale.ENGLISH);
    return country.isBlank()
        ? "%s in %s".formatted(noun, city)
        : "%s in %s, %s".formatted(noun, city, country);
  }

  @Override
  public List<PlaceResult> searchNearby(NearbyQuery query, String languageCode) {
    requireConfigured();
    Map<String, Object> body = new LinkedHashMap<>();
    SearchTextResponse response;
    if (query.hasCenter()) {
      body.put("includedTypes", query.kind().includedTypes());
      body.put("maxResultCount", NEARBY_MAX_RESULTS);
      body.put(
          "locationRestriction",
          Map.of(
              "circle",
              Map.of(
                  "center",
                  Map.of("latitude", query.lat(), "longitude", query.lng()),
                  "radius",
                  query.kind().radiusMeters())));
      body.put("rankPreference", "POPULARITY");
      body.put("languageCode", languageCode);
      response =
          call(
              "searchNearby",
              () ->
                  http.post()
                      .uri("/places:searchNearby")
                      .header("X-Goog-FieldMask", NEARBY_FIELD_MASK)
                      .contentType(MediaType.APPLICATION_JSON)
                      .body(body)
                      .retrieve()
                      .body(SearchTextResponse.class));
    } else {
      // Parada digitada à mão (sem coordenadas): o Nearby exige um centro.
      body.put("textQuery", inCity(query.kind().textNoun(), query.city(), query.countryCode()));
      if (query.kind() == NearbyKind.RESTAURANT) {
        body.put("includedType", "restaurant");
      }
      body.put("languageCode", languageCode);
      body.put("pageSize", NEARBY_MAX_RESULTS);
      response =
          call(
              "searchText",
              () ->
                  http.post()
                      .uri("/places:searchText")
                      .header("X-Goog-FieldMask", NEARBY_FIELD_MASK)
                      .contentType(MediaType.APPLICATION_JSON)
                      .body(body)
                      .retrieve()
                      .body(SearchTextResponse.class));
    }
    if (response == null || response.places() == null) {
      return List.of();
    }
    return response.places().stream()
        .filter(p -> p.id() != null)
        .map(GooglePlacesClient::toResult)
        .toList();
  }

  @Override
  public PlaceInfo getPlaceInfo(String placeId, String languageCode) {
    requireConfigured();
    InfoDetails details;
    try {
      details =
          call(
              "details",
              () ->
                  http.get()
                      .uri(
                          uri ->
                              uri.path("/places/{placeId}")
                                  .queryParam("languageCode", languageCode)
                                  .build(placeId))
                      .header("X-Goog-FieldMask", INFO_FIELD_MASK)
                      .retrieve()
                      .body(InfoDetails.class));
    } catch (PlaceNotFound e) {
      throw new NotFoundException("Lugar não encontrado.");
    }
    if (details == null) {
      throw new NotFoundException("Lugar não encontrado.");
    }
    return new PlaceInfo(
        Objects.requireNonNullElse(details.id(), placeId),
        details.rating(),
        details.userRatingCount(),
        details.location() != null ? details.location().latitude() : null,
        details.location() != null ? details.location().longitude() : null,
        details.googleMapsUri());
  }

  @Override
  public PlaceSummary getPlaceSummary(String placeId, String languageCode) {
    requireConfigured();
    PlaceDetails details;
    try {
      details =
          call(
              "details",
              () ->
                  http.get()
                      .uri(
                          uri ->
                              uri.path("/places/{placeId}")
                                  .queryParam("languageCode", languageCode)
                                  .build(placeId))
                      .header("X-Goog-FieldMask", SUMMARY_FIELD_MASK)
                      .retrieve()
                      .body(PlaceDetails.class));
    } catch (PlaceNotFound e) {
      throw new NotFoundException("Lugar não encontrado.");
    }
    if (details == null) {
      throw new NotFoundException("Lugar não encontrado.");
    }
    return new PlaceSummary(
        Objects.requireNonNullElse(details.id(), placeId),
        text(details.displayName()),
        details.formattedAddress());
  }

  @Override
  public String photoUri(String photoName, int maxWidthPx) {
    requireConfigured();
    PhotoMedia media;
    try {
      media =
          call(
              "details",
              () ->
                  http.get()
                      .uri(
                          uri ->
                              uri.path("/" + photoName + "/media")
                                  .queryParam("maxWidthPx", maxWidthPx)
                                  .queryParam("skipHttpRedirect", true)
                                  .build())
                      .retrieve()
                      .body(PhotoMedia.class));
    } catch (PlaceNotFound e) {
      throw new NotFoundException("Foto não encontrada.");
    }
    if (media == null || media.photoUri() == null || !media.photoUri().startsWith("https://")) {
      throw new NotFoundException("Foto não encontrada.");
    }
    return media.photoUri();
  }

  private static PlaceResult toResult(SearchPlace p) {
    String photo =
        p.photos() == null
            ? null
            : p.photos().stream()
                .map(Photo::name)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    String name = text(p.displayName());
    return new PlaceResult(
        p.id(),
        name != null ? name : p.formattedAddress(),
        p.formattedAddress(),
        p.location() != null ? p.location().latitude() : null,
        p.location() != null ? p.location().longitude() : null,
        p.rating(),
        p.userRatingCount(),
        PriceTierMapper.map(p.priceLevel(), p.primaryType()).orElse(null),
        p.primaryType(),
        photo,
        p.websiteUri(),
        p.googleMapsUri());
  }

  /** {@code shortText} do componente do tipo {@code country} (ISO-2). */
  static String countryCode(List<AddressComponent> components) {
    if (components == null) {
      return null;
    }
    return components.stream()
        .filter(c -> c.types() != null && c.types().contains("country"))
        .map(AddressComponent::shortText)
        .filter(code -> code != null && code.matches("[A-Za-z]{2}"))
        .map(String::toUpperCase)
        .findFirst()
        .orElse(null);
  }

  private static CitySuggestion toSuggestion(PlacePrediction p) {
    StructuredFormat format = p.structuredFormat();
    String main = format != null ? text(format.mainText()) : null;
    String secondary = format != null ? text(format.secondaryText()) : null;
    return new CitySuggestion(p.placeId(), main != null ? main : text(p.text()), secondary);
  }

  private static String text(Text text) {
    return text == null ? null : text.text();
  }

  private void requireConfigured() {
    if (!properties.configured()) {
      throw new ServiceUnavailableException(UNAVAILABLE);
    }
  }

  /**
   * 400/404 nos detalhes (cidade, lugar, foto) → inexistente; qualquer outra falha (429, 5xx, rede)
   * → 503.
   */
  private <T> T call(String operation, Supplier<T> request) {
    try {
      return request.get();
    } catch (HttpStatusCodeException e) {
      HttpStatus status = HttpStatus.resolve(e.getStatusCode().value());
      if ("details".equals(operation)
          && (status == HttpStatus.NOT_FOUND || status == HttpStatus.BAD_REQUEST)) {
        throw new PlaceNotFound();
      }
      log.warn("Places {} falhou com HTTP {}", operation, e.getStatusCode().value());
      throw new ServiceUnavailableException(UNAVAILABLE, e);
    } catch (RestClientException e) {
      log.warn("Places {} falhou: {}", operation, e.getMessage());
      throw new ServiceUnavailableException(UNAVAILABLE, e);
    }
  }

  private static final class PlaceNotFound extends RuntimeException {
    PlaceNotFound() {
      super(null, null, false, false);
    }
  }

  record AutocompleteResponse(List<Suggestion> suggestions) {}

  record Suggestion(PlacePrediction placePrediction) {}

  record PlacePrediction(String placeId, Text text, StructuredFormat structuredFormat) {}

  record StructuredFormat(Text mainText, Text secondaryText) {}

  record Text(String text) {}

  record PlaceDetails(
      String id,
      Text displayName,
      String formattedAddress,
      LatLng location,
      List<AddressComponent> addressComponents) {}

  record LatLng(double latitude, double longitude) {}

  record InfoDetails(
      String id, LatLng location, Double rating, Integer userRatingCount, String googleMapsUri) {}

  record AddressComponent(String longText, String shortText, List<String> types) {}

  record SearchTextResponse(List<SearchPlace> places, String nextPageToken) {}

  record SearchPlace(
      String id,
      Text displayName,
      String formattedAddress,
      LatLng location,
      Double rating,
      Integer userRatingCount,
      String priceLevel,
      String primaryType,
      List<Photo> photos,
      String websiteUri,
      String googleMapsUri) {}

  record Photo(String name) {}

  record PhotoMedia(String name, String photoUri) {}
}
