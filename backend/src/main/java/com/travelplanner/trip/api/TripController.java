package com.travelplanner.trip.api;

import com.travelplanner.auth.domain.AppUserPrincipal;
import com.travelplanner.trip.domain.TripScope;
import com.travelplanner.trip.domain.TripService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** CRUD de viagens. A autorização por papel fica no {@link TripService}. */
@RestController
@RequestMapping("/api/v1/trips")
@RequiredArgsConstructor
class TripController {

  private final TripService trips;

  /** Viagens de que o usuário é membro ({@code scope}: upcoming, past ou all). */
  @GetMapping
  List<TripSummaryResponse> listTrips(
      @AuthenticationPrincipal AppUserPrincipal principal,
      @RequestParam(defaultValue = "upcoming") TripScopeParam scope) {
    return trips.list(principal.getUserId(), scope.toDomain()).stream()
        .map(TripSummaryResponse::from)
        .toList();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  TripResponse createTrip(
      @AuthenticationPrincipal AppUserPrincipal principal,
      @Valid @RequestBody CreateTripRequest request) {
    return TripResponse.from(trips.create(principal.getUserId(), request.toDomain()));
  }

  @GetMapping("/{tripId}")
  TripResponse getTrip(
      @AuthenticationPrincipal AppUserPrincipal principal, @PathVariable long tripId) {
    return TripResponse.from(trips.get(tripId, principal.getUserId()));
  }

  @PutMapping("/{tripId}")
  TripResponse updateTrip(
      @AuthenticationPrincipal AppUserPrincipal principal,
      @PathVariable long tripId,
      @Valid @RequestBody UpdateTripRequest request) {
    return TripResponse.from(
        trips.update(tripId, principal.getUserId(), request.version(), request.toDomain()));
  }

  @DeleteMapping("/{tripId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void deleteTrip(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable long tripId) {
    trips.delete(tripId, principal.getUserId());
  }

  /** Valores aceitos em {@code ?scope=} (minúsculos na URL). */
  enum TripScopeParam {
    upcoming,
    past,
    all;

    TripScope toDomain() {
      return TripScope.valueOf(name().toUpperCase(Locale.ROOT));
    }
  }
}
