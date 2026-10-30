package com.travelplanner.shared.persistence;

import java.time.Clock;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/** Preenche {@code createdAt}/{@code updatedAt} usando o {@link Clock} da aplicação. */
@Configuration(proxyBeanMethods = false)
@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
public class JpaAuditingConfig {

  @Bean
  DateTimeProvider auditingDateTimeProvider(Clock clock) {
    return () -> Optional.of(clock.instant());
  }
}
