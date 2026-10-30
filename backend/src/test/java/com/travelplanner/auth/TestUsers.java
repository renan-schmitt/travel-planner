package com.travelplanner.auth;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;

import com.travelplanner.auth.domain.AppUser;
import com.travelplanner.auth.domain.AppUserPrincipal;
import com.travelplanner.auth.domain.AppUserService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Usuários para testes: o que o Google devolveria e a sessão autenticada correspondente. */
public final class TestUsers {

  private TestUsers() {}

  /** Usuário do Google com {@code sub} aleatório (os testes compartilham o banco). */
  public static OidcUser googleUser(String email, String name) {
    return googleUser("google-" + UUID.randomUUID(), email, name);
  }

  public static OidcUser googleUser(String sub, String email, String name) {
    Instant now = Instant.now();
    OidcIdToken idToken =
        OidcIdToken.withTokenValue("id-token")
            .subject(sub)
            .claim("email", email)
            .claim("name", name)
            .claim("picture", "https://example.com/" + sub + ".png")
            .issuedAt(now)
            .expiresAt(now.plus(1, ChronoUnit.HOURS))
            .build();
    return new DefaultOidcUser(AuthorityUtils.createAuthorityList("OIDC_USER"), idToken);
  }

  /** Provisiona o usuário (como no login real) e devolve a sessão autenticada para o MockMvc. */
  public static RequestPostProcessor loggedIn(AppUserService users, String email) {
    return loggedIn(users, googleUser(email, "Usuário de Teste"));
  }

  public static RequestPostProcessor loggedIn(AppUserService users, OidcUser googleUser) {
    AppUser user = users.upsertFromOidc(googleUser);
    return oidcLogin().oidcUser(new AppUserPrincipal(user.getId(), googleUser));
  }
}
