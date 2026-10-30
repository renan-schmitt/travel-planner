package com.travelplanner.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.travelplanner.IntegrationTest;
import com.travelplanner.auth.TestUsers;
import com.travelplanner.auth.infra.AppUserRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/** Provisionamento do usuário no login com Google (o delegate simula a resposta do Google). */
class AppOidcUserServiceIT extends IntegrationTest {

  @Autowired AppUserService users;

  @Autowired AppUserRepository repository;

  @Test
  void firstLoginCreatesUser() {
    String sub = "google-" + UUID.randomUUID();

    OidcUser principal =
        service(TestUsers.googleUser(sub, "ana@example.com", "Ana")).loadUser(null);

    assertThat(principal).isInstanceOf(AppUserPrincipal.class);
    var user = repository.findByGoogleSub(sub).orElseThrow();
    assertThat(((AppUserPrincipal) principal).getUserId()).isEqualTo(user.getId());
    assertThat(principal.getName()).isEqualTo(user.getId().toString());
    assertThat(user.getEmail()).isEqualTo("ana@example.com");
    assertThat(user.getName()).isEqualTo("Ana");
    assertThat(user.getPictureUrl()).isEqualTo("https://example.com/" + sub + ".png");
    assertThat(user.getNationality()).isEqualTo("BR");
    assertThat(user.getLocale()).isEqualTo("pt-BR");
    assertThat(user.getCreatedAt()).isNotNull();
  }

  @Test
  void secondLoginUpdatesUserWithoutDuplicating() {
    String sub = "google-" + UUID.randomUUID();
    service(TestUsers.googleUser(sub, "bia@example.com", "Bia")).loadUser(null);
    long countAfterFirst = repository.count();

    var principal =
        (AppUserPrincipal)
            service(TestUsers.googleUser(sub, "bia.nova@example.com", "Beatriz")).loadUser(null);

    assertThat(repository.count()).isEqualTo(countAfterFirst);
    var user = repository.findById(principal.getUserId()).orElseThrow();
    assertThat(user.getGoogleSub()).isEqualTo(sub);
    assertThat(user.getName()).isEqualTo("Beatriz");
    assertThat(user.getEmail()).isEqualTo("bia.nova@example.com");
  }

  private AppOidcUserService service(OidcUser fromGoogle) {
    return new AppOidcUserService(request -> fromGoogle, users);
  }
}
