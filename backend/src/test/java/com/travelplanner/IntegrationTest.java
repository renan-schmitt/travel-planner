package com.travelplanner;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.mysql.MySQLContainer;

/**
 * Base dos testes de integração: contexto completo + MySQL 8 em Testcontainers. O container é
 * estático e iniciado uma única vez, compartilhado por todas as subclasses.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTest {

  @ServiceConnection
  static final MySQLContainer MYSQL =
      new MySQLContainer("mysql:8.0")
          .withCommand("--character-set-server=utf8mb4", "--collation-server=utf8mb4_0900_ai_ci");

  static {
    MYSQL.start();
  }
}
