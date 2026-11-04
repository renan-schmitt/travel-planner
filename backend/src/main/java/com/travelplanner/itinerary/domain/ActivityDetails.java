package com.travelplanner.itinerary.domain;

import com.travelplanner.shared.web.InvalidRequestException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Campos editáveis de uma atividade. Horários opcionais, no relógio local do dia; {@code endTime}
 * só com {@code startTime}. {@code savedPlaceId} liga a atividade a um lugar favorito.
 */
public record ActivityDetails(
    LocalDate day,
    LocalTime startTime,
    LocalTime endTime,
    String title,
    String notes,
    Long savedPlaceId) {

  private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM");

  /** O mesmo com outro título (ex.: o nome do favorito, quando nenhum foi digitado). */
  public ActivityDetails withTitle(String title) {
    return new ActivityDetails(day, startTime, endTime, title, notes, savedPlaceId);
  }

  /**
   * Dia dentro da viagem e fim depois do início.
   *
   * @throws InvalidRequestException com o campo que precisa ser corrigido
   */
  public void validate(LocalDate tripStart, LocalDate tripEnd) {
    checkDay(day, tripStart, tripEnd);
    if (endTime != null && startTime == null) {
      throw new InvalidRequestException("startTime", "Informe o horário de início.");
    }
    if (endTime != null && !endTime.isAfter(startTime)) {
      throw new InvalidRequestException("endTime", "O fim deve ser depois do início.");
    }
  }

  /** {@code day} entre o início e o fim da viagem (sem fim, a partir do início). */
  public static void checkDay(LocalDate day, LocalDate tripStart, LocalDate tripEnd) {
    if (tripStart == null) {
      throw new InvalidRequestException(
          "day", "Defina as datas da viagem antes de planejar atividades.");
    }
    if (day.isBefore(tripStart) || (tripEnd != null && day.isAfter(tripEnd))) {
      throw new InvalidRequestException(
          "day",
          tripEnd == null
              ? "Escolha um dia a partir de %s.".formatted(DAY.format(tripStart))
              : "Escolha um dia da viagem (%s a %s)."
                  .formatted(DAY.format(tripStart), DAY.format(tripEnd)));
    }
  }
}
