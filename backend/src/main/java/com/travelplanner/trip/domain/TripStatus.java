package com.travelplanner.trip.domain;

import java.time.LocalDate;

/**
 * Status da viagem. O usuário só grava {@link #PLANNING} ou {@link #CONFIRMED}; {@link
 * #IN_PROGRESS} e {@link #DONE} são calculados na leitura a partir das datas (ver {@link
 * #effective}).
 */
public enum TripStatus {
  PLANNING,
  CONFIRMED,
  IN_PROGRESS,
  DONE;

  /**
   * Status exibido: {@code DONE} depois do último dia, {@code IN_PROGRESS} a partir do primeiro dia
   * (inclusive) e, antes disso, o status gravado. Sem data de fim a viagem nunca fica {@code DONE}
   * sozinha; sem datas, vale o gravado.
   */
  public static TripStatus effective(
      TripStatus stored, LocalDate startDate, LocalDate endDate, LocalDate today) {
    if (endDate != null && today.isAfter(endDate)) {
      return DONE;
    }
    if (startDate != null && !today.isBefore(startDate)) {
      return IN_PROGRESS;
    }
    return stored;
  }
}
