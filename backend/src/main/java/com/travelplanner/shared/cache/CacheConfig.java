package com.travelplanner.shared.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.List;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Caches em memória (Caffeine), cada um com o próprio tempo de vida e tamanho máximo. */
@Configuration(proxyBeanMethods = false)
@EnableCaching
public class CacheConfig {

  /** Autocomplete de cidades, por texto normalizado. */
  public static final String PLACES_AUTOCOMPLETE = "places-autocomplete";

  /** Detalhes de uma cidade, por {@code placeId}. */
  public static final String PLACES_CITY = "places-city";

  /** Página da busca de hospedagem, por {@code LodgingQuery} (parada + filtros + página). */
  public static final String PLACES_LODGING_SEARCH = "places-lodging-search";

  /**
   * Faixa de preço que a busca mostrou para um {@code placeId}: ao adicionar o resultado à viagem,
   * a hospedagem guarda a origem certa (Places ou estimada).
   */
  public static final String PLACES_LODGING_TIER = "places-lodging-tier";

  /** Nome e endereço de um lugar, por {@code placeId}. */
  public static final String PLACES_SUMMARY = "places-summary";

  /** URL pública de uma foto, por nome + largura. */
  public static final String PLACES_PHOTO = "places-photo";

  @Bean
  CacheManager cacheManager() {
    CaffeineCacheManager manager = new CaffeineCacheManager();
    // Só os caches registrados aqui existem (nome desconhecido → erro, em vez de cache sem limite).
    manager.setCacheNames(List.of());
    manager.registerCustomCache(
        PLACES_AUTOCOMPLETE,
        Caffeine.newBuilder().expireAfterWrite(Duration.ofMinutes(10)).maximumSize(1_000).build());
    manager.registerCustomCache(
        PLACES_CITY,
        Caffeine.newBuilder().expireAfterWrite(Duration.ofHours(24)).maximumSize(5_000).build());
    manager.registerCustomCache(
        PLACES_LODGING_SEARCH,
        Caffeine.newBuilder().expireAfterWrite(Duration.ofHours(1)).maximumSize(500).build());
    manager.registerCustomCache(
        PLACES_LODGING_TIER,
        Caffeine.newBuilder().expireAfterWrite(Duration.ofHours(1)).maximumSize(10_000).build());
    manager.registerCustomCache(
        PLACES_SUMMARY,
        Caffeine.newBuilder().expireAfterWrite(Duration.ofHours(24)).maximumSize(2_000).build());
    manager.registerCustomCache(
        PLACES_PHOTO,
        Caffeine.newBuilder().expireAfterWrite(Duration.ofHours(1)).maximumSize(5_000).build());
    return manager;
  }
}
