package com.travelplanner.auth.domain;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuração da autenticação ({@code app.auth.*}).
 *
 * @param postLoginRedirect para onde o navegador vai depois do login com Google (em dev, o Vite)
 * @param loginErrorRedirect para onde vai se o login com Google falhar
 * @param adminEmails e-mails com acesso de administrador ({@code APP_ADMIN_EMAILS})
 */
@ConfigurationProperties("app.auth")
public record AuthProperties(
    @DefaultValue("/") String postLoginRedirect,
    @DefaultValue("/login?error") String loginErrorRedirect,
    @DefaultValue List<String> adminEmails) {

  public boolean isAdmin(String email) {
    return email != null
        && adminEmails.stream().map(String::trim).anyMatch(email::equalsIgnoreCase);
  }
}
