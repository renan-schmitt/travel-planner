package com.travelplanner.dashboard.api;

import com.travelplanner.auth.domain.AppUserPrincipal;
import com.travelplanner.dashboard.domain.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Tela inicial: o que importa agora nas viagens do usuário. */
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
class DashboardController {

  private final DashboardService dashboard;

  @GetMapping
  DashboardResponse getDashboard(@AuthenticationPrincipal AppUserPrincipal principal) {
    return DashboardResponse.from(dashboard.get(principal.getUserId()));
  }
}
