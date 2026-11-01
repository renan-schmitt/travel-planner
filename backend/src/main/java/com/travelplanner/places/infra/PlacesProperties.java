package com.travelplanner.places.infra;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuração do Google Places ({@code app.places.*}).
 *
 * @param apiKey chave restrita à "Places API (New)" ({@code PLACES_API_KEY}); vazia → 503 e o
 *     usuário digita a cidade à mão
 * @param baseUrl raiz da API (os testes apontam para o WireMock)
 */
@ConfigurationProperties("app.places")
public record PlacesProperties(
    String apiKey,
    @DefaultValue("https://places.googleapis.com/v1") String baseUrl,
    @DefaultValue("3s") Duration connectTimeout,
    @DefaultValue("10s") Duration readTimeout) {

  public boolean configured() {
    return apiKey != null && !apiKey.isBlank();
  }
}
