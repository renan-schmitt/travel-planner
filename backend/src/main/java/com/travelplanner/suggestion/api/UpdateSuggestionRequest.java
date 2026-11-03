package com.travelplanner.suggestion.api;

import com.travelplanner.suggestion.domain.SuggestionStatus;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * Decisão sobre a sugestão; {@code snoozedUntil} obrigatório (e depois de hoje) em {@code SNOOZED}.
 */
public record UpdateSuggestionRequest(@NotNull SuggestionStatus status, LocalDate snoozedUntil) {}
