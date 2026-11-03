package com.travelplanner.suggestion.api;

import com.travelplanner.auth.domain.AppUserPrincipal;
import com.travelplanner.suggestion.domain.SuggestionService;
import com.travelplanner.suggestion.domain.SuggestionStatus;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Sugestões do motor de lacunas. A autorização por papel fica no {@link SuggestionService}. */
@RestController
@RequestMapping("/api/v1/trips/{tripId}/suggestions")
@RequiredArgsConstructor
class SuggestionController {

  private final SuggestionService suggestions;

  /** Mais graves primeiro. {@code status} filtra pelo estado efetivo (ausente = todas). */
  @GetMapping
  List<SuggestionResponse> listSuggestions(
      @AuthenticationPrincipal AppUserPrincipal principal,
      @PathVariable long tripId,
      @RequestParam(required = false) SuggestionStatus status) {
    return suggestions.list(tripId, principal.getUserId(), status).stream()
        .map(SuggestionResponse::from)
        .toList();
  }

  /** Aceitar, adiar, dispensar ou reabrir (idempotente, sem {@code version}). */
  @PatchMapping("/{suggestionId}")
  SuggestionResponse updateSuggestion(
      @AuthenticationPrincipal AppUserPrincipal principal,
      @PathVariable long tripId,
      @PathVariable long suggestionId,
      @Valid @RequestBody UpdateSuggestionRequest request) {
    return SuggestionResponse.from(
        suggestions.decide(
            tripId, suggestionId, principal.getUserId(), request.status(), request.snoozedUntil()));
  }
}
