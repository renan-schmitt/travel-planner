package com.travelplanner.shared.web;

/**
 * Regra de negócio violada por um campo do pedido (ex.: paradas sobrepostas) → 400, no mesmo
 * formato dos erros de validação ({@code errors: [{field, message}]}).
 */
public class InvalidRequestException extends RuntimeException {

  private final String field;

  public InvalidRequestException(String field, String message) {
    super(message);
    this.field = field;
  }

  public String getField() {
    return field;
  }
}
