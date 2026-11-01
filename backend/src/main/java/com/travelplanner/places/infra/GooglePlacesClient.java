package com.travelplanner.places.infra;

import com.travelplanner.places.domain.CitySuggestion;
import com.travelplanner.places.domain.PlaceCity;
import com.travelplanner.places.domain.PlacesClient;
import com.travelplanner.shared.web.NotFoundException;
import com.travelplanner.shared.web.ServiceUnavailableException;
import java.net.http.HttpClient;
import java.util.LinkedHashMap;
import java.util.List;
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
    } catch (CityNotFound e) {
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

  /** 400/404 nos detalhes → cidade inexistente; qualquer outra falha (429, 5xx, rede) → 503. */
  private <T> T call(String operation, Supplier<T> request) {
    try {
      return request.get();
    } catch (HttpStatusCodeException e) {
      HttpStatus status = HttpStatus.resolve(e.getStatusCode().value());
      if ("details".equals(operation)
          && (status == HttpStatus.NOT_FOUND || status == HttpStatus.BAD_REQUEST)) {
        throw new CityNotFound();
      }
      log.warn("Places {} falhou com HTTP {}", operation, e.getStatusCode().value());
      throw new ServiceUnavailableException(UNAVAILABLE, e);
    } catch (RestClientException e) {
      log.warn("Places {} falhou: {}", operation, e.getMessage());
      throw new ServiceUnavailableException(UNAVAILABLE, e);
    }
  }

  private static final class CityNotFound extends RuntimeException {
    CityNotFound() {
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

  record AddressComponent(String longText, String shortText, List<String> types) {}
}
