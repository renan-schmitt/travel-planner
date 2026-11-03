package com.travelplanner.suggestion.domain;

/** Severidade da sugestão, da menor para a maior (a ordem importa: {@link #compareTo}). */
public enum Severity {
  INFO,
  WARNING,
  CRITICAL
}
