package com.travelplanner.auth.dev;

import com.travelplanner.auth.domain.AppUser;
import com.travelplanner.auth.domain.AppUserPrincipal;
import com.travelplanner.auth.domain.AppUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Login de desenvolvimento, sem Google: {@code POST /dev/login?email=...} cria (ou reaproveita) o
 * usuário e autentica a sessão. Para testes manuais, E2E e sessões sem acesso ao Google.
 *
 * <p>Só existe nos perfis {@code local} e {@code test}; o {@link DevLoginGuard} impede a subida da
 * aplicação se ele aparecer com o perfil {@code prod}. O CSRF continua exigido.
 */
@Slf4j
@RestController
@Profile("local | test")
@RequiredArgsConstructor
class DevLoginController {

  static final String REGISTRATION_ID = "dev";
  static final String SUBJECT_PREFIX = "dev:";

  private final AppUserService users;
  private final Clock clock;
  private final SecurityContextRepository securityContextRepository =
      new HttpSessionSecurityContextRepository();

  @PostMapping("/dev/login")
  ResponseEntity<Void> devLogin(
      @RequestParam @NotBlank @Email String email,
      HttpServletRequest request,
      HttpServletResponse response) {
    Instant now = clock.instant();
    OidcIdToken idToken =
        OidcIdToken.withTokenValue("dev-login")
            .subject(SUBJECT_PREFIX + email.toLowerCase())
            .claim(StandardClaimNames.EMAIL, email)
            .claim(StandardClaimNames.NAME, email.substring(0, email.indexOf('@')))
            .issuedAt(now)
            .expiresAt(now.plus(Duration.ofDays(30)))
            .build();
    var oidcUser = new DefaultOidcUser(AuthorityUtils.createAuthorityList("OIDC_USER"), idToken);
    AppUser user = users.upsertFromOidc(oidcUser);
    var principal = new AppUserPrincipal(user.getId(), oidcUser);

    // Proteção contra fixação de sessão, como no login real.
    if (request.getSession(false) != null) {
      request.changeSessionId();
    }
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(
        new OAuth2AuthenticationToken(principal, principal.getAuthorities(), REGISTRATION_ID));
    SecurityContextHolder.setContext(context);
    securityContextRepository.saveContext(context, request, response);
    log.info("Login de desenvolvimento: usuário {} ({})", user.getId(), email);
    return ResponseEntity.noContent().build();
  }
}
