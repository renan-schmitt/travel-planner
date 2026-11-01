package com.travelplanner.trip.api;

import com.travelplanner.auth.domain.AppUserPrincipal;
import com.travelplanner.trip.domain.Traveler;
import com.travelplanner.trip.domain.TravelerService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Viajantes do usuário e quem vai em cada viagem. A autorização fica no {@link TravelerService}.
 */
@RestController
@RequiredArgsConstructor
class TravelerController {

  private final TravelerService travelers;

  @GetMapping("/api/v1/travelers")
  List<TravelerResponse> listTravelers(@AuthenticationPrincipal AppUserPrincipal principal) {
    return toResponse(travelers.list(principal.getUserId()), principal);
  }

  @PostMapping("/api/v1/travelers")
  @ResponseStatus(HttpStatus.CREATED)
  TravelerResponse createTraveler(
      @AuthenticationPrincipal AppUserPrincipal principal,
      @Valid @RequestBody TravelerRequest request) {
    return TravelerResponse.from(
        travelers.create(principal.getUserId(), request.toDomain()), principal.getUserId());
  }

  @PutMapping("/api/v1/travelers/{travelerId}")
  TravelerResponse updateTraveler(
      @AuthenticationPrincipal AppUserPrincipal principal,
      @PathVariable long travelerId,
      @Valid @RequestBody UpdateTravelerRequest request) {
    Traveler traveler =
        travelers.update(travelerId, principal.getUserId(), request.version(), request.toDomain());
    return TravelerResponse.from(traveler, principal.getUserId());
  }

  @DeleteMapping("/api/v1/travelers/{travelerId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void deleteTraveler(
      @AuthenticationPrincipal AppUserPrincipal principal, @PathVariable long travelerId) {
    travelers.delete(travelerId, principal.getUserId());
  }

  @GetMapping("/api/v1/trips/{tripId}/travelers")
  List<TravelerResponse> listTripTravelers(
      @AuthenticationPrincipal AppUserPrincipal principal, @PathVariable long tripId) {
    return toResponse(travelers.listForTrip(tripId, principal.getUserId()), principal);
  }

  @PutMapping("/api/v1/trips/{tripId}/travelers")
  List<TravelerResponse> setTripTravelers(
      @AuthenticationPrincipal AppUserPrincipal principal,
      @PathVariable long tripId,
      @Valid @RequestBody SetTripTravelersRequest request) {
    return toResponse(
        travelers.setForTrip(tripId, principal.getUserId(), request.travelerIds()), principal);
  }

  private static List<TravelerResponse> toResponse(
      List<Traveler> list, AppUserPrincipal principal) {
    return list.stream().map(t -> TravelerResponse.from(t, principal.getUserId())).toList();
  }
}
