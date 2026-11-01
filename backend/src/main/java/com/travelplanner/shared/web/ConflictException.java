package com.travelplanner.shared.web;

/** Operação que contraria o estado atual do recurso (ex.: excluir o viajante "eu") → 409. */
public class ConflictException extends RuntimeException {

  public ConflictException(String message) {
    super(message);
  }
}
