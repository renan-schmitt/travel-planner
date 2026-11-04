package com.travelplanner.itinerary.domain;

import com.travelplanner.shared.web.InvalidRequestException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Campos editáveis de uma hospedagem. Nome em texto livre ou vindo da busca no Places; {@code
 * priceTier} de 1 ({@code $}) a 4 ({@code $$$$}), ou {@code null} = "não sei". Na casa de um amigo
 * ou parente ({@link LodgingKind#FRIENDS_HOME}) regime, faixa de preço e reserva são descartados.
 */
public record LodgingDetails(
    LodgingKind kind,
    String name,
    String address,
    LocalDate checkIn,
    LocalDate checkOut,
    BoardType boardType,
    Integer priceTier,
    String bookingRef,
    String notes) {

  private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM");

  public LodgingDetails {
    if (kind == LodgingKind.FRIENDS_HOME) {
      boardType = null;
      priceTier = null;
      bookingRef = null;
    }
  }

  /** Os mesmos campos com outro nome e endereço (ex.: completados pelo Places). */
  public LodgingDetails withPlace(String name, String address) {
    return new LodgingDetails(
        kind, name, address, checkIn, checkOut, boardType, priceTier, bookingRef, notes);
  }

  public int nights() {
    return (int) ChronoUnit.DAYS.between(checkIn, checkOut);
  }

  /** Cabe nas datas da parada (pode coincidir com a chegada e a saída). */
  public boolean fitsIn(LocalDate arrival, LocalDate departure) {
    return !checkIn.isBefore(arrival) && !checkOut.isAfter(departure);
  }

  /**
   * Check-out depois do check-in e as duas datas dentro (ou no limite) das datas da parada.
   *
   * @throws InvalidRequestException com o campo que precisa ser corrigido
   */
  public void validate(LocalDate stopArrival, LocalDate stopDeparture) {
    if (!checkOut.isAfter(checkIn)) {
      throw new InvalidRequestException("checkOut", "O check-out deve ser depois do check-in.");
    }
    String stay =
        "Fique dentro das datas da parada (%s a %s)."
            .formatted(DAY.format(stopArrival), DAY.format(stopDeparture));
    if (checkIn.isBefore(stopArrival)) {
      throw new InvalidRequestException("checkIn", "Check-in antes da chegada. " + stay);
    }
    if (checkOut.isAfter(stopDeparture)) {
      throw new InvalidRequestException("checkOut", "Check-out depois da saída. " + stay);
    }
  }
}
