package com.travelplanner.auth.api;

import com.travelplanner.auth.domain.AppUser;
import com.travelplanner.auth.domain.AppUserPrincipal;
import com.travelplanner.auth.domain.AppUserService;
import com.travelplanner.auth.domain.AuthProperties;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
class MeController {

  private final AppUserService users;
  private final AuthProperties authProperties;

  @GetMapping
  MeResponse getMe(@AuthenticationPrincipal AppUserPrincipal principal) {
    return toResponse(users.get(principal.getUserId()));
  }

  @PatchMapping
  MeResponse updateMe(
      @AuthenticationPrincipal AppUserPrincipal principal,
      @Valid @RequestBody UpdateMeRequest request) {
    AppUser user =
        users.updateProfile(
            principal.getUserId(),
            request.version(),
            request.nationality(),
            request.homeCity() == null ? null : request.homeCity().toDomain());
    return toResponse(user);
  }

  private MeResponse toResponse(AppUser user) {
    return MeResponse.from(user, authProperties.isAdmin(user.getEmail()));
  }
}
