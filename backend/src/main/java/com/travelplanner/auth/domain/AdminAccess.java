package com.travelplanner.auth.domain;

import com.travelplanner.shared.web.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Administradores = e-mails em {@code app.auth.admin-emails} ({@code APP_ADMIN_EMAILS}). */
@Component
@RequiredArgsConstructor
public class AdminAccess {

  private final AppUserService users;
  private final AuthProperties properties;

  public boolean isAdmin(long userId) {
    return properties.isAdmin(users.get(userId).getEmail());
  }

  /** 403 se o usuário não for administrador. */
  public void requireAdmin(long userId) {
    if (!isAdmin(userId)) {
      throw new ForbiddenException("Só administradores podem fazer isso.");
    }
  }
}
