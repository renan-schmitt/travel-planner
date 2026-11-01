package com.travelplanner.shared.web;

/** Dependência externa fora do ar ou sem quota (ex.: Google Places) → 503. */
public class ServiceUnavailableException extends RuntimeException {

  public ServiceUnavailableException(String message) {
    super(message);
  }

  public ServiceUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
