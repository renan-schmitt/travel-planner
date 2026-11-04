package com.travelplanner.itinerary.api;

import com.travelplanner.auth.domain.AppUserPrincipal;
import com.travelplanner.itinerary.domain.SavedPlace;
import com.travelplanner.itinerary.domain.SavedPlaceService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lugares favoritos das paradas: CRUD por parada (a listagem traz nota e posição do Places) e uma
 * listagem da viagem inteira, sem os dados do Places.
 */
@RestController
@RequestMapping("/api/v1/trips/{tripId}")
@RequiredArgsConstructor
class SavedPlaceController {

  private final SavedPlaceService saved;

  @GetMapping("/saved-places")
  List<SavedPlaceResponse> listTripSavedPlaces(
      @AuthenticationPrincipal AppUserPrincipal principal, @PathVariable long tripId) {
    return saved.listByTrip(tripId, principal.getUserId()).stream()
        .map(SavedPlaceResponse::from)
        .toList();
  }

  @GetMapping("/stops/{stopId}/saved-places")
  List<SavedPlaceResponse> listSavedPlaces(
      @AuthenticationPrincipal AppUserPrincipal principal,
      @PathVariable long tripId,
      @PathVariable long stopId) {
    List<SavedPlace> list = saved.list(tripId, stopId, principal.getUserId());
    var info = saved.placeInfo(list);
    return list.stream().map(p -> SavedPlaceResponse.from(p, info.get(p.getPlaceId()))).toList();
  }

  @PostMapping("/stops/{stopId}/saved-places")
  @ResponseStatus(HttpStatus.CREATED)
  SavedPlaceResponse createSavedPlace(
      @AuthenticationPrincipal AppUserPrincipal principal,
      @PathVariable long tripId,
      @PathVariable long stopId,
      @Valid @RequestBody SavedPlaceRequest request) {
    return SavedPlaceResponse.from(
        saved.create(
            tripId,
            stopId,
            principal.getUserId(),
            request.placeId(),
            request.kind(),
            SegmentRequest.clean(request.name()),
            request.priceTier(),
            SegmentRequest.clean(request.notes())));
  }

  @PatchMapping("/stops/{stopId}/saved-places/{savedPlaceId}")
  SavedPlaceResponse updateSavedPlace(
      @AuthenticationPrincipal AppUserPrincipal principal,
      @PathVariable long tripId,
      @PathVariable long stopId,
      @PathVariable long savedPlaceId,
      @Valid @RequestBody UpdateSavedPlaceRequest request) {
    return SavedPlaceResponse.from(
        saved.update(
            tripId,
            stopId,
            savedPlaceId,
            principal.getUserId(),
            request.version(),
            request.kind(),
            request.priceTier(),
            SegmentRequest.clean(request.notes()),
            request.visited()));
  }

  @DeleteMapping("/stops/{stopId}/saved-places/{savedPlaceId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void deleteSavedPlace(
      @AuthenticationPrincipal AppUserPrincipal principal,
      @PathVariable long tripId,
      @PathVariable long stopId,
      @PathVariable long savedPlaceId) {
    saved.delete(tripId, stopId, savedPlaceId, principal.getUserId());
  }
}
