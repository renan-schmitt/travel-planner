package com.travelplanner.places.domain;

import com.travelplanner.places.infra.TimeZoneResolver;
import com.travelplanner.shared.cache.CacheConfig;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

/**
 * Autocomplete e detalhes de cidades, com cache (autocomplete 10 min, detalhes 24 h) e fuso
 * calculado localmente a partir das coordenadas; busca de hospedagem, de restaurantes/atrações e
 * fotos (cache de 1 h); nota/posição de um lugar (24 h). Falhas não entram no cache.
 */
@Service
@RequiredArgsConstructor
public class PlacesService {

  /** Idioma dos nomes devolvidos pelo Places (a interface só tem pt-BR por enquanto). */
  static final String LANGUAGE = "pt-BR";

  private final PlacesClient client;
  private final TimeZoneResolver timeZones;
  private final CacheManager cacheManager;

  @SuppressWarnings("unchecked")
  public List<CitySuggestion> autocompleteCities(String input, String sessionToken) {
    String key = normalize(input);
    Cache cache = cache(CacheConfig.PLACES_AUTOCOMPLETE);
    List<CitySuggestion> suggestions = cache.get(key, List.class);
    if (suggestions == null) {
      suggestions = List.copyOf(client.autocompleteCities(input.strip(), sessionToken, LANGUAGE));
      cache.put(key, suggestions);
    }
    // O Places respondeu: carrega os dados de fuso em paralelo, para os detalhes da cidade
    // escolhida saírem rápido (com o Places fora do ar, a cidade é digitada com o fuso).
    timeZones.warmUpAsync();
    return suggestions;
  }

  public City getCity(String placeId, String sessionToken) {
    Cache cache = cache(CacheConfig.PLACES_CITY);
    City cached = cache.get(placeId, City.class);
    if (cached != null) {
      return cached;
    }
    PlaceCity place = client.getCity(placeId, sessionToken, LANGUAGE);
    City city =
        new City(
            place.placeId(),
            place.name(),
            place.countryCode(),
            place.lat(),
            place.lng(),
            timeZones.resolve(place.lat(), place.lng()).orElse(null));
    cache.put(placeId, city);
    return city;
  }

  /**
   * Hospedagens perto da cidade (cache de 1 h por consulta; os resultados não são persistidos). A
   * faixa de cada resultado fica lembrada por {@code placeId}, para {@link #searchedTier}.
   */
  public LodgingPage searchLodging(LodgingQuery query) {
    Cache cache = cache(CacheConfig.PLACES_LODGING_SEARCH);
    LodgingPage page = cache.get(query, LodgingPage.class);
    if (page == null) {
      LodgingPage found = client.searchLodging(query, LANGUAGE);
      page = new LodgingPage(List.copyOf(found.places()), found.nextPageToken());
      cache.put(query, page);
    }
    rememberTiers(page.places());
    return page;
  }

  /**
   * Restaurantes/atrações perto do centro (cache de 1 h por consulta; não persistidos). A faixa de
   * cada resultado fica lembrada por {@code placeId}, para {@link #searchedTier}.
   */
  @SuppressWarnings("unchecked")
  public List<PlaceResult> searchNearby(NearbyQuery query) {
    Cache cache = cache(CacheConfig.PLACES_NEARBY_SEARCH);
    List<PlaceResult> results = cache.get(query, List.class);
    if (results == null) {
      results = List.copyOf(client.searchNearby(query, LANGUAGE));
      cache.put(query, results);
    }
    rememberTiers(results);
    return results;
  }

  /** Nota, posição e link do Maps (cache de 24 h). */
  public PlaceInfo getPlaceInfo(String placeId) {
    Cache cache = cache(CacheConfig.PLACES_INFO);
    PlaceInfo info = cache.get(placeId, PlaceInfo.class);
    if (info == null) {
      info = client.getPlaceInfo(placeId, LANGUAGE);
      cache.put(placeId, info);
    }
    return info;
  }

  private void rememberTiers(List<PlaceResult> results) {
    Cache tiers = cache(CacheConfig.PLACES_SEARCH_TIER);
    results.stream()
        .filter(p -> p.priceTier() != null)
        .forEach(p -> tiers.put(p.placeId(), p.priceTier()));
  }

  /** Faixa que uma busca recente (até 1 h) mostrou para o lugar. */
  public Optional<PriceTier> searchedTier(String placeId) {
    return Optional.ofNullable(cache(CacheConfig.PLACES_SEARCH_TIER).get(placeId, PriceTier.class));
  }

  /** Nome e endereço de um lugar (cache de 24 h). */
  public PlaceSummary getPlaceSummary(String placeId) {
    Cache cache = cache(CacheConfig.PLACES_SUMMARY);
    PlaceSummary summary = cache.get(placeId, PlaceSummary.class);
    if (summary == null) {
      summary = client.getPlaceSummary(placeId, LANGUAGE);
      cache.put(placeId, summary);
    }
    return summary;
  }

  /** URL pública da foto (cache de 1 h; cada foto é cobrada). */
  public String photoUri(String photoName, int maxWidthPx) {
    Cache cache = cache(CacheConfig.PLACES_PHOTO);
    String key = photoName + "@" + maxWidthPx;
    String uri = cache.get(key, String.class);
    if (uri == null) {
      uri = client.photoUri(photoName, maxWidthPx);
      cache.put(key, uri);
    }
    return uri;
  }

  /** " São Paulo " e "são paulo" usam a mesma entrada do cache. */
  static String normalize(String input) {
    return input.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
  }

  private Cache cache(String name) {
    Cache cache = cacheManager.getCache(name);
    if (cache == null) {
      throw new IllegalStateException("Cache não configurado: " + name);
    }
    return cache;
  }
}
