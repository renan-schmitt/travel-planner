package com.travelplanner.itinerary.domain;

import com.travelplanner.shared.web.InvalidRequestException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * Campos editáveis de um trecho. De deslocamento: {@code originStopId} → {@code destinationStopId},
 * onde {@code null} é a cidade de origem do usuário ("casa"). Local: só {@code localStopId}
 * (transfer, aluguel de carro dentro de uma parada). Horários no relógio local de cada ponta, com o
 * fuso IANA ao lado; a chegada é opcional.
 */
public record SegmentDetails(
    TransportType type,
    Long originStopId,
    Long destinationStopId,
    Long localStopId,
    String originLabel,
    String destinationLabel,
    LocalDateTime departAt,
    String departTz,
    LocalDateTime arriveAt,
    String arriveTz,
    String carrier,
    String code,
    String bookingRef,
    String notes) {

  public boolean local() {
    return localStopId != null;
  }

  public ZonedDateTime departure() {
    return departAt.atZone(ZoneId.of(departTz));
  }

  /** Instante da chegada, ou {@code null} se ela não foi informada. */
  public ZonedDateTime arrival() {
    return arriveAt == null ? null : arriveAt.atZone(ZoneId.of(arriveTz));
  }

  /**
   * Regras entre campos: trecho local ou de deslocamento (nunca os dois), origem ≠ destino, fuso
   * junto da chegada e chegada depois da partida no tempo real (não no relógio local: um voo Lisboa
   * → São Paulo pode "chegar antes" no relógio e ainda assim ser válido).
   *
   * @throws InvalidRequestException com o campo que precisa ser corrigido
   */
  public void validate() {
    if (local() && (originStopId != null || destinationStopId != null)) {
      throw new InvalidRequestException(
          "localStopId", "Um trecho local não tem origem nem destino.");
    }
    if (!local() && Objects.equals(originStopId, destinationStopId)) {
      throw new InvalidRequestException(
          "destinationStopId", "O destino deve ser diferente da origem.");
    }
    if (arriveAt != null && arriveTz == null) {
      throw new InvalidRequestException("arriveTz", "Informe o fuso da chegada.");
    }
    if (arriveAt != null && !arrival().toInstant().isAfter(departure().toInstant())) {
      throw new InvalidRequestException(
          "arriveAt", "A chegada deve ser depois da partida (considerando os fusos).");
    }
  }
}
