package com.travelplanner.suggestion.domain;

import com.travelplanner.trip.domain.TripChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Roda o motor depois que uma alteração da viagem é gravada: síncrono, numa transação nova (o
 * volume é pequeno, sem debounce). Uma falha no motor não desfaz nem quebra a alteração do usuário:
 * só vai para o log, e a próxima alteração reconcilia de novo.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class SuggestionTrigger {

  private final SuggestionEngine engine;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  void onTripChanged(TripChangedEvent event) {
    try {
      engine.run(event.tripId());
    } catch (RuntimeException e) {
      log.error("Motor de lacunas falhou na viagem {}", event.tripId(), e);
    }
  }
}
