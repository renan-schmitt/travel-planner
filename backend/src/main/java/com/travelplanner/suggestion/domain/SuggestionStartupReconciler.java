package com.travelplanner.suggestion.domain;

import com.travelplanner.trip.infra.TripRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Ao subir a aplicação, recalcula as sugestões de todas as viagens que ainda não terminaram. Cobre
 * viagens criadas antes do motor existir (nunca avaliadas) e as regras que dependem da data ({@code
 * CHECKIN_ONLINE}, adiamentos vencidos) até o job diário da Fase 20. Roda numa thread virtual, para
 * não atrasar a inicialização; cada viagem numa transação própria, e a falha de uma não impede as
 * outras.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SuggestionStartupReconciler {

  private final SuggestionProperties properties;
  private final TripRepository trips;
  private final SuggestionEngine engine;
  private final Clock clock;

  @EventListener(ApplicationReadyEvent.class)
  void onReady() {
    if (properties.reconcileOnStartup()) {
      Thread.ofVirtual().name("suggestions-startup").start(this::reconcileAll);
    }
  }

  /**
   * Recalcula as viagens não encerradas (fim a partir de ontem em UTC, folga para qualquer fuso).
   *
   * @return quantas viagens foram recalculadas com sucesso
   */
  public int reconcileAll() {
    LocalDate since = LocalDate.now(clock.withZone(ZoneOffset.UTC)).minusDays(1);
    int done = 0;
    int failed = 0;
    for (long tripId : trips.findIdsNotEndedBefore(since)) {
      try {
        engine.run(tripId);
        done++;
      } catch (RuntimeException e) {
        failed++;
        log.error("Motor de lacunas falhou na viagem {} (inicialização)", tripId, e);
      }
    }
    log.info("Sugestões recalculadas na inicialização: {} viagens ({} com erro)", done, failed);
    return done;
  }
}
