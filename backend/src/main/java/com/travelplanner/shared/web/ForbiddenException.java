package com.travelplanner.shared.web;

/** Usuário autenticado sem permissão para a operação → 403. */
public class ForbiddenException extends RuntimeException {

  public ForbiddenException(String message) {
    super(message);
  }
}
