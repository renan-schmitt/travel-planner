package com.travelplanner.itinerary.api;

import com.travelplanner.auth.domain.AppUserPrincipal;
import com.travelplanner.itinerary.domain.Lodging;
import com.travelplanner.itinerary.domain.LodgingService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Hospedagens das paradas. CRUD por parada e uma listagem da viagem inteira (para a aba Roteiro não
 * fazer um pedido por parada). A autorização por papel fica no {@link LodgingService}.
 */
@RestController
@RequestMapping("/api/v1/trips/{tripId}")
@RequiredArgsConstructor
class LodgingController {

  private final LodgingService lodgings;

  /** Hospedagens de todas as paradas, por check-in. */
  @GetMapping("/lodgings")
  List<LodgingResponse> listTripLodgings(
      @AuthenticationPrincipal AppUserPrincipal principal, @PathVariable long tripId) {
    return toResponse(lodgings.listByTrip(tripId, principal.getUserId()));
  }

  @GetMapping("/stops/{stopId}/lodgings")
  List<LodgingResponse> listLodgings(
      @AuthenticationPrincipal AppUserPrincipal principal,
      @PathVariable long tripId,
      @PathVariable long stopId) {
    return toResponse(lodgings.list(tripId, stopId, principal.getUserId()));
  }

  @PostMapping("/stops/{stopId}/lodgings")
  @ResponseStatus(HttpStatus.CREATED)
  LodgingResponse createLodging(
      @AuthenticationPrincipal AppUserPrincipal principal,
      @PathVariable long tripId,
      @PathVariable long stopId,
      @Valid @RequestBody LodgingRequest request) {
    return LodgingResponse.from(
        lodgings.create(tripId, stopId, principal.getUserId(), request.toDomain()));
  }

  @PutMapping("/stops/{stopId}/lodgings/{lodgingId}")
  LodgingResponse updateLodging(
      @AuthenticationPrincipal AppUserPrincipal principal,
      @PathVariable long tripId,
      @PathVariable long stopId,
      @PathVariable long lodgingId,
      @Valid @RequestBody UpdateLodgingRequest request) {
    return LodgingResponse.from(
        lodgings.update(
            tripId,
            stopId,
            lodgingId,
            principal.getUserId(),
            request.version(),
            request.toDomain()));
  }

  @DeleteMapping("/stops/{stopId}/lodgings/{lodgingId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void deleteLodging(
      @AuthenticationPrincipal AppUserPrincipal principal,
      @PathVariable long tripId,
      @PathVariable long stopId,
      @PathVariable long lodgingId) {
    lodgings.delete(tripId, stopId, lodgingId, principal.getUserId());
  }

  private static List<LodgingResponse> toResponse(List<Lodging> list) {
    return list.stream().map(LodgingResponse::from).toList();
  }
}
