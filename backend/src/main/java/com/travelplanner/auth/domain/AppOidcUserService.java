package com.travelplanner.auth.domain;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

/**
 * Carrega o usuário do Google (delegando ao {@link OidcUserService} padrão), provisiona o {@link
 * AppUser} e devolve um {@link AppUserPrincipal}.
 */
@Component
public class AppOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

  private final OAuth2UserService<OidcUserRequest, OidcUser> delegate;
  private final AppUserService appUserService;

  @Autowired
  public AppOidcUserService(AppUserService appUserService) {
    this(new OidcUserService(), appUserService);
  }

  AppOidcUserService(
      OAuth2UserService<OidcUserRequest, OidcUser> delegate, AppUserService appUserService) {
    this.delegate = delegate;
    this.appUserService = appUserService;
  }

  @Override
  public OidcUser loadUser(OidcUserRequest userRequest) {
    OidcUser oidcUser = delegate.loadUser(userRequest);
    AppUser user = appUserService.upsertFromOidc(oidcUser);
    return new AppUserPrincipal(user.getId(), oidcUser);
  }
}
