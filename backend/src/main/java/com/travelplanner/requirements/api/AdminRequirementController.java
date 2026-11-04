package com.travelplanner.requirements.api;

import com.travelplanner.auth.domain.AppUserPrincipal;
import com.travelplanner.requirements.domain.RequirementAdminService;
import com.travelplanner.requirements.domain.RequirementStatus;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Base de requisitos de documentos (só administradores; 403 para os demais). */
@RestController
@RequestMapping("/api/v1/admin/requirements")
@RequiredArgsConstructor
class AdminRequirementController {

  private final RequirementAdminService service;

  @GetMapping
  List<RequirementResponse> listRequirements(
      @AuthenticationPrincipal AppUserPrincipal principal,
      @RequestParam(required = false) String countryCode,
      @RequestParam(required = false) RequirementStatus status) {
    return service.list(principal.getUserId(), countryCode, status).stream()
        .map(RequirementResponse::from)
        .toList();
  }

  @GetMapping("/{id}")
  RequirementResponse getRequirement(
      @AuthenticationPrincipal AppUserPrincipal principal, @PathVariable long id) {
    return RequirementResponse.from(service.get(principal.getUserId(), id));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  RequirementResponse createRequirement(
      @AuthenticationPrincipal AppUserPrincipal principal,
      @Valid @RequestBody RequirementRequest request) {
    return RequirementResponse.from(service.create(principal.getUserId(), request.toDomain()));
  }

  @PutMapping("/{id}")
  RequirementResponse updateRequirement(
      @AuthenticationPrincipal AppUserPrincipal principal,
      @PathVariable long id,
      @Valid @RequestBody UpdateRequirementRequest request) {
    return RequirementResponse.from(
        service.update(principal.getUserId(), id, request.version(), request.toDomain()));
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void deleteRequirement(
      @AuthenticationPrincipal AppUserPrincipal principal, @PathVariable long id) {
    service.delete(principal.getUserId(), id);
  }

  /** Publica (ou confirma de novo na fonte oficial): {@code verifiedAt} = hoje. */
  @PostMapping("/{id}/publish")
  RequirementResponse publishRequirement(
      @AuthenticationPrincipal AppUserPrincipal principal, @PathVariable long id) {
    return RequirementResponse.from(service.publish(principal.getUserId(), id));
  }

  /** Volta para rascunho (deixa de valer para as viagens). */
  @PostMapping("/{id}/unpublish")
  RequirementResponse unpublishRequirement(
      @AuthenticationPrincipal AppUserPrincipal principal, @PathVariable long id) {
    return RequirementResponse.from(service.unpublish(principal.getUserId(), id));
  }
}
