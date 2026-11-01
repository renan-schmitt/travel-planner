package com.travelplanner.itinerary.api;

import com.travelplanner.auth.domain.AppUserPrincipal;
import com.travelplanner.itinerary.domain.Stop;
import com.travelplanner.itinerary.domain.StopService;
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

/** Paradas (roteiro) da viagem. A autorização por papel fica no {@link StopService}. */
@RestController
@RequestMapping("/api/v1/trips/{tripId}/stops")
@RequiredArgsConstructor
class StopController {

  private final StopService stops;

  /** Paradas na ordem do roteiro. */
  @GetMapping
  List<StopResponse> listStops(
      @AuthenticationPrincipal AppUserPrincipal principal, @PathVariable long tripId) {
    return toResponse(stops.list(tripId, principal.getUserId()));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  StopResponse createStop(
      @AuthenticationPrincipal AppUserPrincipal principal,
      @PathVariable long tripId,
      @Valid @RequestBody StopRequest request) {
    return StopResponse.from(stops.create(tripId, principal.getUserId(), request.toDomain()));
  }

  /** Nova ordem (todos os ids). Declarado antes de {@code /{stopId}} só por clareza. */
  @PutMapping("/order")
  List<StopResponse> reorderStops(
      @AuthenticationPrincipal AppUserPrincipal principal,
      @PathVariable long tripId,
      @Valid @RequestBody ReorderStopsRequest request) {
    return toResponse(stops.reorder(tripId, principal.getUserId(), request.stopIds()));
  }

  @PutMapping("/{stopId}")
  StopResponse updateStop(
      @AuthenticationPrincipal AppUserPrincipal principal,
      @PathVariable long tripId,
      @PathVariable long stopId,
      @Valid @RequestBody UpdateStopRequest request) {
    return StopResponse.from(
        stops.update(tripId, stopId, principal.getUserId(), request.version(), request.toDomain()));
  }

  @DeleteMapping("/{stopId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void deleteStop(
      @AuthenticationPrincipal AppUserPrincipal principal,
      @PathVariable long tripId,
      @PathVariable long stopId) {
    stops.delete(tripId, stopId, principal.getUserId());
  }

  private static List<StopResponse> toResponse(List<Stop> list) {
    return list.stream().map(StopResponse::from).toList();
  }
}
