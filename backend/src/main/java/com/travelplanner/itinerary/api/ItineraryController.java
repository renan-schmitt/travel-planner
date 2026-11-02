package com.travelplanner.itinerary.api;

import com.travelplanner.auth.domain.AppUserPrincipal;
import com.travelplanner.itinerary.domain.ItineraryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Timeline dia a dia da viagem (leitura). */
@RestController
@RequestMapping("/api/v1/trips/{tripId}/itinerary")
@RequiredArgsConstructor
class ItineraryController {

  private final ItineraryService itinerary;

  @GetMapping
  ItineraryResponse getItinerary(
      @AuthenticationPrincipal AppUserPrincipal principal, @PathVariable long tripId) {
    return ItineraryResponse.from(itinerary.build(tripId, principal.getUserId()));
  }
}
