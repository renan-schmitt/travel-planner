package com.travelplanner.suggestion.domain;

import com.travelplanner.itinerary.infra.StopRepository;
import com.travelplanner.requirements.domain.RequirementsChangedEvent;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Requisito do país mudou: recalcula as sugestões das viagens não encerradas com parada nele
 * (síncrono, cada viagem na sua transação; a falha de uma só vai para o log).
 */
@Slf4j
@Component
@RequiredArgsConstructor
class RequirementsChangedTrigger {

  private final StopRepository stops;
  private final SuggestionEngine engine;
  private final Clock clock;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  void onRequirementsChanged(RequirementsChangedEvent event) {
    LocalDate since = LocalDate.now(clock.withZone(ZoneOffset.UTC)).minusDays(1);
    for (long tripId : stops.findTripIdsInCountryNotEndedBefore(event.countryCode(), since)) {
      try {
        engine.run(tripId);
      } catch (RuntimeException e) {
        log.error(
            "Motor de lacunas falhou na viagem {} (requisitos de {})",
            tripId,
            event.countryCode(),
            e);
      }
    }
  }
}
