package com.travelplanner.places.api;

import com.travelplanner.places.domain.PlacesService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.net.URI;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Proxy das fotos do Places: pede ao Google a URL pública da foto ({@code skipHttpRedirect}) e
 * redireciona o navegador para ela. A chave da API nunca chega ao cliente.
 */
@RestController
@RequestMapping("/api/v1/places/photos")
@RequiredArgsConstructor
class PlacePhotoController {

  private final PlacesService places;

  /**
   * @param name {@code places/<placeId>/photos/<photoId>}, como veio na busca
   * @param w largura máxima em pixels
   */
  @GetMapping
  ResponseEntity<Void> photo(
      @RequestParam
          @NotBlank
          @Pattern(regexp = "places/[A-Za-z0-9_-]{1,255}/photos/[A-Za-z0-9_-]{1,1024}")
          String name,
      @RequestParam(defaultValue = "400") @Min(100) @Max(1600) int w) {
    return ResponseEntity.status(HttpStatus.FOUND)
        .location(URI.create(places.photoUri(name, w)))
        .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePrivate())
        .build();
  }
}
