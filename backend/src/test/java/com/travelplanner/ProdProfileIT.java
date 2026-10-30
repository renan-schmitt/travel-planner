package com.travelplanner;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Sobe a aplicação com o perfil {@code prod} (inicialização preguiçosa, cookies {@code Secure}, log
 * em JSON). Só o banco é trocado: o {@code @ServiceConnection} do Testcontainers tem precedência
 * sobre a URL do Cloud SQL.
 */
@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    properties = {
      "CLOUD_SQL_INSTANCE=projeto:regiao:instancia",
      "DB_USERNAME=ignorado",
      "DB_PASSWORD=ignorado",
      "GOOGLE_CLIENT_ID=prod-client-id",
      "GOOGLE_CLIENT_SECRET=prod-client-secret"
    })
@ActiveProfiles(value = "prod", inheritProfiles = false)
class ProdProfileIT extends IntegrationTest {

  @LocalServerPort int port;

  @Autowired ApplicationContext context;

  @Autowired JdbcTemplate jdbc;

  private final HttpClient http = HttpClient.newHttpClient();

  @Test
  void startsWithFlywayAppliedAndHealthUp() throws Exception {
    Integer pending =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 0", Integer.class);
    Integer applied =
        jdbc.queryForObject("SELECT COUNT(*) FROM flyway_schema_history", Integer.class);
    assertThat(applied).isPositive();
    assertThat(pending).isZero();

    HttpResponse<String> health = get("/actuator/health", false);
    assertThat(health.statusCode()).isEqualTo(200);
    assertThat(health.body()).contains("\"status\":\"UP\"");
  }

  @Test
  void devLoginDoesNotExist() {
    assertThat(context.containsBean("devLoginController")).isFalse();
  }

  @Test
  void oauthBehindCloudRunProxyUsesHttpsAndSecureCookie() throws Exception {
    HttpResponse<String> response = get("/oauth2/authorization/google", true);

    assertThat(response.statusCode()).isEqualTo(302);
    String location = response.headers().firstValue("Location").orElseThrow();
    assertThat(location)
        .startsWith("https://accounts.google.com/")
        .contains("client_id=prod-client-id")
        .contains("redirect_uri=https://app.example.com/login/oauth2/code/google");
    assertThat(response.headers().allValues("Set-Cookie"))
        .filteredOn(header -> header.startsWith("SESSION="))
        .singleElement()
        .asString()
        .contains("Secure")
        .contains("HttpOnly");
  }

  /** {@code behindProxy}: simula os headers que o front-end do Cloud Run envia. */
  private HttpResponse<String> get(String path, boolean behindProxy) throws Exception {
    HttpRequest.Builder request =
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
    if (behindProxy) {
      request.header("X-Forwarded-Proto", "https").header("X-Forwarded-Host", "app.example.com");
    }
    return http.send(request.build(), BodyHandlers.ofString());
  }
}
