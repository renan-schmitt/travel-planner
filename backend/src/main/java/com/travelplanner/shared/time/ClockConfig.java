package com.travelplanner.shared.time;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Hora atual sempre via {@link Clock} injetado, para ser substituível nos testes. */
@Configuration(proxyBeanMethods = false)
public class ClockConfig {

  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }
}
