package com.travelplanner.checklist.api;

import com.travelplanner.auth.domain.AppUserPrincipal;
import com.travelplanner.checklist.domain.ChecklistCategory;
import com.travelplanner.checklist.domain.ChecklistFilter;
import com.travelplanner.checklist.domain.ChecklistItem;
import com.travelplanner.checklist.domain.ChecklistService;
import com.travelplanner.checklist.domain.ChecklistStatus;
import com.travelplanner.shared.web.InvalidRequestException;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Checklist da viagem. A autorização por papel fica no {@link ChecklistService}. */
@RestController
@RequestMapping("/api/v1/trips/{tripId}/checklist")
@RequiredArgsConstructor
class ChecklistController {

  private final ChecklistService checklist;

  /**
   * Itens na ordem do usuário. Filtros opcionais: {@code category}, {@code status} e {@code
   * assignee=me} (só os itens de que quem pede é responsável).
   */
  @GetMapping
  List<ChecklistItemResponse> listChecklist(
      @AuthenticationPrincipal AppUserPrincipal principal,
      @PathVariable long tripId,
      @RequestParam(required = false) ChecklistCategory category,
      @RequestParam(required = false) ChecklistStatus status,
      @RequestParam(required = false) String assignee) {
    if (assignee != null && !assignee.equals("me")) {
      throw new InvalidRequestException("assignee", "Use assignee=me.");
    }
    var filter = new ChecklistFilter(category, status, assignee != null);
    return toResponse(checklist.list(tripId, principal.getUserId(), filter));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  ChecklistItemResponse createChecklistItem(
      @AuthenticationPrincipal AppUserPrincipal principal,
      @PathVariable long tripId,
      @Valid @RequestBody ChecklistItemRequest request) {
    return ChecklistItemResponse.from(
        checklist.create(tripId, principal.getUserId(), request.toDomain()));
  }

  /** Nova ordem (todos os ids). Declarado antes de {@code /{itemId}} por clareza. */
  @PutMapping("/order")
  List<ChecklistItemResponse> reorderChecklist(
      @AuthenticationPrincipal AppUserPrincipal principal,
      @PathVariable long tripId,
      @Valid @RequestBody ReorderChecklistRequest request) {
    return toResponse(checklist.reorder(tripId, principal.getUserId(), request.itemIds()));
  }

  @PutMapping("/{itemId}")
  ChecklistItemResponse updateChecklistItem(
      @AuthenticationPrincipal AppUserPrincipal principal,
      @PathVariable long tripId,
      @PathVariable long itemId,
      @Valid @RequestBody UpdateChecklistItemRequest request) {
    return ChecklistItemResponse.from(
        checklist.update(
            tripId, itemId, principal.getUserId(), request.version(), request.toDomain()));
  }

  /** Conclui ou reabre o item (idempotente, sem {@code version}). */
  @PatchMapping("/{itemId}/status")
  ChecklistItemResponse setChecklistItemStatus(
      @AuthenticationPrincipal AppUserPrincipal principal,
      @PathVariable long tripId,
      @PathVariable long itemId,
      @Valid @RequestBody ChecklistStatusRequest request) {
    return ChecklistItemResponse.from(
        checklist.changeStatus(tripId, itemId, principal.getUserId(), request.status()));
  }

  @DeleteMapping("/{itemId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void deleteChecklistItem(
      @AuthenticationPrincipal AppUserPrincipal principal,
      @PathVariable long tripId,
      @PathVariable long itemId) {
    checklist.delete(tripId, itemId, principal.getUserId());
  }

  private static List<ChecklistItemResponse> toResponse(List<ChecklistItem> list) {
    return list.stream().map(ChecklistItemResponse::from).toList();
  }
}
