package com.travelplanner.suggestion.domain;

/** Estado da sugestão: aceita, adiada até uma data ou dispensada (não volta). */
public enum SuggestionStatus {
  PENDING,
  ACCEPTED,
  SNOOZED,
  DISMISSED
}
