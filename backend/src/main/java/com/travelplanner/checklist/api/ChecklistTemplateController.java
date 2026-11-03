package com.travelplanner.checklist.api;

import com.travelplanner.auth.domain.AppUserPrincipal;
import com.travelplanner.checklist.domain.ChecklistTemplateService;
import com.travelplanner.checklist.domain.TemplateApplyResult;
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

/** Templates de checklist e a aplicação deles numa viagem. */
@RestController
@RequiredArgsConstructor
class ChecklistTemplateController {

  private final ChecklistTemplateService templates;

  /** Do sistema (primeiro) e do usuário, com os itens. */
  @GetMapping("/api/v1/checklist-templates")
  List<TemplateResponse> listChecklistTemplates(
      @AuthenticationPrincipal AppUserPrincipal principal) {
    return templates.list(principal.getUserId()).stream().map(TemplateResponse::from).toList();
  }

  @GetMapping("/api/v1/checklist-templates/{templateId}")
  TemplateResponse getChecklistTemplate(
      @AuthenticationPrincipal AppUserPrincipal principal, @PathVariable long templateId) {
    return TemplateResponse.from(templates.get(templateId, principal.getUserId()));
  }

  @PostMapping("/api/v1/checklist-templates")
  @ResponseStatus(HttpStatus.CREATED)
  TemplateResponse createChecklistTemplate(
      @AuthenticationPrincipal AppUserPrincipal principal,
      @Valid @RequestBody TemplateRequest request) {
    return TemplateResponse.from(templates.create(principal.getUserId(), request.toDomain()));
  }

  /** Só templates do usuário; do sistema → 403. */
  @PutMapping("/api/v1/checklist-templates/{templateId}")
  TemplateResponse updateChecklistTemplate(
      @AuthenticationPrincipal AppUserPrincipal principal,
      @PathVariable long templateId,
      @Valid @RequestBody UpdateTemplateRequest request) {
    return TemplateResponse.from(
        templates.update(templateId, principal.getUserId(), request.version(), request.toDomain()));
  }

  @DeleteMapping("/api/v1/checklist-templates/{templateId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void deleteChecklistTemplate(
      @AuthenticationPrincipal AppUserPrincipal principal, @PathVariable long templateId) {
    templates.delete(templateId, principal.getUserId());
  }

  @PostMapping("/api/v1/trips/{tripId}/checklist/apply-template")
  ApplyTemplateResponse applyChecklistTemplate(
      @AuthenticationPrincipal AppUserPrincipal principal,
      @PathVariable long tripId,
      @Valid @RequestBody ApplyTemplateRequest request) {
    TemplateApplyResult result =
        templates.apply(tripId, request.templateId(), principal.getUserId());
    return new ApplyTemplateResponse(
        result.added().stream().map(ChecklistItemResponse::from).toList(), result.skipped());
  }

  @PostMapping("/api/v1/trips/{tripId}/checklist/save-as-template")
  @ResponseStatus(HttpStatus.CREATED)
  TemplateResponse saveChecklistAsTemplate(
      @AuthenticationPrincipal AppUserPrincipal principal,
      @PathVariable long tripId,
      @Valid @RequestBody SaveAsTemplateRequest request) {
    return TemplateResponse.from(
        templates.saveTripAsTemplate(tripId, principal.getUserId(), request.name().strip()));
  }
}
