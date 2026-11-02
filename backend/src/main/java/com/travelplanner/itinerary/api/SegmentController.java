package com.travelplanner.itinerary.api;

import com.travelplanner.auth.domain.AppUserPrincipal;
import com.travelplanner.itinerary.domain.SegmentService;
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

/** Trechos de transporte da viagem. A autorização por papel fica no {@link SegmentService}. */
@RestController
@RequestMapping("/api/v1/trips/{tripId}/segments")
@RequiredArgsConstructor
class SegmentController {

  private final SegmentService segments;

  /** Trechos em ordem cronológica de partida. */
  @GetMapping
  List<SegmentResponse> listSegments(
      @AuthenticationPrincipal AppUserPrincipal principal, @PathVariable long tripId) {
    return segments.list(tripId, principal.getUserId()).stream()
        .map(SegmentResponse::from)
        .toList();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  SegmentResponse createSegment(
      @AuthenticationPrincipal AppUserPrincipal principal,
      @PathVariable long tripId,
      @Valid @RequestBody SegmentRequest request) {
    return SegmentResponse.from(segments.create(tripId, principal.getUserId(), request.toDomain()));
  }

  @PutMapping("/{segmentId}")
  SegmentResponse updateSegment(
      @AuthenticationPrincipal AppUserPrincipal principal,
      @PathVariable long tripId,
      @PathVariable long segmentId,
      @Valid @RequestBody UpdateSegmentRequest request) {
    return SegmentResponse.from(
        segments.update(
            tripId, segmentId, principal.getUserId(), request.version(), request.toDomain()));
  }

  @DeleteMapping("/{segmentId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void deleteSegment(
      @AuthenticationPrincipal AppUserPrincipal principal,
      @PathVariable long tripId,
      @PathVariable long segmentId) {
    segments.delete(tripId, segmentId, principal.getUserId());
  }
}
