package com.travelplanner.auth.domain;

import java.io.Serial;
import lombok.Getter;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/**
 * Usuário autenticado: o {@link OidcUser} do Google mais o id do {@link AppUser}. Controllers
 * recebem com {@code @AuthenticationPrincipal AppUserPrincipal}.
 *
 * <p>Fica serializado na sessão (Spring Session JDBC), por isso é {@code Serializable} e tem {@code
 * serialVersionUID} fixo. {@link #getName()} devolve o id, que vai para a coluna {@code
 * PRINCIPAL_NAME} da sessão.
 */
@Getter
public class AppUserPrincipal extends DefaultOidcUser {

  @Serial private static final long serialVersionUID = 1L;

  private final long userId;

  public AppUserPrincipal(long userId, OidcUser oidcUser) {
    super(
        oidcUser.getAuthorities(),
        oidcUser.getIdToken(),
        oidcUser.getUserInfo(),
        IdTokenClaimNames.SUB);
    this.userId = userId;
  }

  @Override
  public String getName() {
    return Long.toString(userId);
  }
}
