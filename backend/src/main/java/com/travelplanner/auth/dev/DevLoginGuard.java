package com.travelplanner.auth.dev;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

/**
 * Falha a inicialização se o {@link DevLoginController} existir com o perfil {@code prod} ativo
 * (por exemplo, {@code SPRING_PROFILES_ACTIVE=prod,local} por engano).
 */
@Component
class DevLoginGuard implements ApplicationListener<ContextRefreshedEvent> {

  @Override
  public void onApplicationEvent(ContextRefreshedEvent event) {
    ApplicationContext context = event.getApplicationContext();
    if (context.getEnvironment().matchesProfiles("prod")
        && context.getBeanNamesForType(DevLoginController.class).length > 0) {
      throw new IllegalStateException(
          "O login de desenvolvimento (DevLoginController) não pode existir no perfil prod.");
    }
  }
}
