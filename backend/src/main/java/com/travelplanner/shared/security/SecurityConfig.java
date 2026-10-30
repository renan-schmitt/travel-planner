package com.travelplanner.shared.security;

import com.travelplanner.auth.domain.AppOidcUserService;
import com.travelplanner.auth.domain.AuthProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;

/**
 * Login com Google (OIDC) e sessão por cookie (Spring Session JDBC).
 *
 * <ul>
 *   <li>{@code /api/**} exige sessão; sem ela → 401 sem corpo (a SPA redireciona para {@code
 *       /login}).
 *   <li>O resto (SPA, assets, {@code /oauth2/**}, {@code /login/**}, {@code /actuator/health}) é
 *       público.
 *   <li>CSRF para SPA: cookie {@code XSRF-TOKEN} legível pelo JS, enviado de volta no header {@code
 *       X-XSRF-TOKEN}. O token é gravado em toda resposta.
 *   <li>{@code POST /logout} → 204.
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
public class SecurityConfig {

  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http, AppOidcUserService oidcUserService, AuthProperties authProperties)
      throws Exception {
    return http.authorizeHttpRequests(
            auth -> auth.requestMatchers("/api/**").authenticated().anyRequest().permitAll())
        // Depois do login vamos sempre para post-login-redirect; sem o cache, um 401 anônimo não
        // cria sessão no banco.
        .requestCache(cache -> cache.disable())
        .exceptionHandling(
            e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
        .oauth2Login(
            oauth ->
                oauth
                    // A página /login é da SPA; sem isto o Spring gera a própria.
                    .loginPage("/login")
                    .userInfoEndpoint(userInfo -> userInfo.oidcUserService(oidcUserService))
                    .defaultSuccessUrl(authProperties.postLoginRedirect(), true)
                    .failureUrl(authProperties.loginErrorRedirect()))
        .logout(
            logout ->
                logout
                    .logoutUrl("/logout")
                    .logoutSuccessHandler(
                        new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))
        .csrf(csrf -> csrf.spa())
        .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
        .headers(
            headers ->
                headers.referrerPolicy(
                    referrer -> referrer.policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)))
        .build();
  }
}
