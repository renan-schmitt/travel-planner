package com.travelplanner.places.infra;

import java.time.ZoneId;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import net.iakovlev.timeshape.TimeZoneEngine;
import org.springframework.stereotype.Component;

/**
 * Fuso horário IANA a partir de latitude/longitude, sem chamar API paga: usa os dados offline da
 * biblioteca timeshape. Os dados ocupam dezenas de MB em memória e levam alguns segundos para
 * carregar, então são carregados só no primeiro uso (ou no {@link #warmUpAsync}).
 */
@Slf4j
@Component
public class TimeZoneResolver {

  private volatile TimeZoneEngine engine;
  private volatile boolean warmingUp;

  /** Fuso do ponto; vazio se não houver (ex.: alto-mar) ou as coordenadas forem inválidas. */
  public Optional<String> resolve(double lat, double lng) {
    if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
      return Optional.empty();
    }
    return engine().query(lat, lng).map(ZoneId::getId);
  }

  /** Começa a carregar os dados numa thread virtual, se ainda não estiverem carregados. */
  public void warmUpAsync() {
    if (engine == null && !warmingUp) {
      warmingUp = true;
      Thread.ofVirtual().name("timeshape-warm-up").start(this::engine);
    }
  }

  private TimeZoneEngine engine() {
    TimeZoneEngine current = engine;
    if (current == null) {
      synchronized (this) {
        current = engine;
        if (current == null) {
          long start = System.nanoTime();
          current = TimeZoneEngine.initialize();
          engine = current;
          log.info(
              "Dados de fuso horário carregados em {} ms", (System.nanoTime() - start) / 1_000_000);
        }
      }
    }
    return current;
  }
}
