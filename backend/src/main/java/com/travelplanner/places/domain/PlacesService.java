package com.travelplanner.places.domain;

import com.travelplanner.places.infra.TimeZoneResolver;
import com.travelplanner.shared.cache.CacheConfig;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

/**
 * Autocomplete e detalhes de cidades, com cache (autocomplete 10 min, detalhes 24 h) e fuso
 * calculado localmente a partir das coordenadas. Falhas não entram no cache.
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
