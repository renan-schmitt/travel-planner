package com.travelplanner.shared.web;

/**
 * Recurso inexistente → 404. Usada também quando o usuário não tem acesso à viagem, para não
 * revelar que ela existe.
 */
public class NotFoundException extends RuntimeException {

  public NotFoundException(String message) {
    super(message);
  }
}
