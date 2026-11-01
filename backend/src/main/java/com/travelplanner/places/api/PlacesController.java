package com.travelplanner.places.api;

import com.travelplanner.places.domain.PlacesService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Busca de cidades no Google Places. O {@code session} (UUID gerado pelo frontend a cada interação)
 * agrupa o autocomplete e os detalhes numa sessão de cobrança. Places fora do ar → 503 (o frontend
 * oferece a digitação manual).
 */
@RestController
@RequestMapping("/api/v1/places/cities")
@RequiredArgsConstructor
class PlacesController {

  private static final String SESSION_PATTERN = "[A-Za-z0-9-]{1,64}";

  private final PlacesService places;

  /** Cidades para o texto digitado (mínimo 3 caracteres; o frontend faz debounce). */
  @GetMapping("/autocomplete")
  List<CitySuggestionResponse> autocompleteCities(
      @RequestParam @NotBlank @Size(min = 3, max = 100) String q,
      @RequestParam(required = false) @Pattern(regexp = SESSION_PATTERN) String session) {
    return places.autocompleteCities(q, session).stream()
        .map(CitySuggestionResponse::from)
        .toList();
  }

  /** País, coordenadas e fuso da cidade escolhida; encerra a sessão do autocomplete. */
  @GetMapping("/{placeId}")
  CityResponse getCity(
      @PathVariable @Pattern(regexp = "[A-Za-z0-9_-]{1,255}") String placeId,
      @RequestParam(required = false) @Pattern(regexp = SESSION_PATTERN) String session) {
    return CityResponse.from(places.getCity(placeId, session));
  }
}
