package com.travelplanner.auth.dev;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.travelplanner.auth.domain.AppUserService;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class DevLoginGuardTest {

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner()
          .withBean(AppUserService.class, () -> mock(AppUserService.class))
          .withBean(Clock.class, Clock::systemUTC)
          .withUserConfiguration(DevLoginController.class, DevLoginGuard.class);

  @Test
  void devLoginDoesNotExistInProdProfile() {
    runner
        .withPropertyValues("spring.profiles.active=prod")
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              assertThat(context).doesNotHaveBean(DevLoginController.class);
            });
  }

  @Test
  void devLoginExistsInLocalAndTestProfiles() {
    runner
        .withPropertyValues("spring.profiles.active=local")
        .run(context -> assertThat(context).hasSingleBean(DevLoginController.class));
    runner
        .withPropertyValues("spring.profiles.active=test")
        .run(context -> assertThat(context).hasSingleBean(DevLoginController.class));
  }

  @Test
  void startupFailsIfDevLoginExistsWithProdProfile() {
    // `prod,local` por engano: o @Profile("local | test") aceita, o guarda barra.
    runner
        .withPropertyValues("spring.profiles.active=prod,local")
        .run(
            context ->
                assertThat(context).hasFailed().getFailure().hasMessageContaining("perfil prod"));
  }
}
