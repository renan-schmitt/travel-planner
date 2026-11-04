package com.travelplanner.itinerary.api;

import com.travelplanner.auth.domain.AppUserPrincipal;
import com.travelplanner.itinerary.domain.ActivityService;
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

/** Atividades da timeline: CRUD e reordenação de um dia (arrastar e soltar). */
@RestController
@RequestMapping("/api/v1/trips/{tripId}/activities")
@RequiredArgsConstructor
class ActivityController {

  private final ActivityService activities;

  /** Por dia e posição (a timeline ordena as com horário pelo horário). */
  @GetMapping
  List<ActivityResponse> listActivities(
      @AuthenticationPrincipal AppUserPrincipal principal, @PathVariable long tripId) {
    return activities.list(tripId, principal.getUserId()).stream()
        .map(ActivityResponse::from)
        .toList();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  ActivityResponse createActivity(
      @AuthenticationPrincipal AppUserPrincipal principal,
      @PathVariable long tripId,
      @Valid @RequestBody ActivityRequest request) {
    return ActivityResponse.from(
        activities.create(tripId, principal.getUserId(), request.toDomain()));
  }

  /** Declarado antes de {@code /{activityId}} só por clareza. */
  @PutMapping("/reorder")
  List<ActivityResponse> reorderActivities(
      @AuthenticationPrincipal AppUserPrincipal principal,
      @PathVariable long tripId,
      @Valid @RequestBody ReorderActivitiesRequest request) {
    return activities
        .reorder(tripId, principal.getUserId(), request.day(), request.activityIds())
        .stream()
        .map(ActivityResponse::from)
        .toList();
  }

  @PutMapping("/{activityId}")
  ActivityResponse updateActivity(
      @AuthenticationPrincipal AppUserPrincipal principal,
      @PathVariable long tripId,
      @PathVariable long activityId,
      @Valid @RequestBody UpdateActivityRequest request) {
    return ActivityResponse.from(
        activities.update(
            tripId, activityId, principal.getUserId(), request.version(), request.toDomain()));
  }

  @DeleteMapping("/{activityId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void deleteActivity(
      @AuthenticationPrincipal AppUserPrincipal principal,
      @PathVariable long tripId,
      @PathVariable long activityId) {
    activities.delete(tripId, activityId, principal.getUserId());
  }
}
