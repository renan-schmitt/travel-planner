package com.travelplanner.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.travelplanner.IntegrationTest;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * Fluxo da SPA por HTTP de verdade (servidor numa porta aleatória): o primeiro {@code GET} recebe o
 * cookie {@code XSRF-TOKEN}, o {@code POST} devolve o token no header e a sessão vem num cookie
 * {@code HttpOnly}.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class CookiesOverHttpIT extends IntegrationTest {

  @LocalServerPort int port;

  private final CookieManager cookies = new CookieManager();
  private final HttpClient http = HttpClient.newBuilder().cookieHandler(cookies).build();

  @Test
  void spaLoginFlowWithCsrfAndSessionCookies() throws Exception {
    HttpResponse<String> anonymous = send(HttpRequest.newBuilder(uri("/api/v1/me")));
    assertThat(anonymous.statusCode()).isEqualTo(401);
    assertThat(setCookie(anonymous, "XSRF-TOKEN")).doesNotContainIgnoringCase("HttpOnly");
    assertThat(setCookie(anonymous, "SESSION")).as("401 anônimo não cria sessão").isNull();

    HttpRequest.Builder login =
        HttpRequest.newBuilder(uri("/dev/login?email=julia@example.com"))
            .POST(HttpRequest.BodyPublishers.noBody());
    assertThat(send(login.copy()).statusCode()).as("sem o header CSRF").isEqualTo(403);

    HttpResponse<String> loggedIn = send(login.header("X-XSRF-TOKEN", cookie("XSRF-TOKEN")));
    assertThat(loggedIn.statusCode()).isEqualTo(204);
    assertThat(setCookie(loggedIn, "SESSION"))
        .contains("HttpOnly")
        .contains("SameSite=Lax")
        .contains("Max-Age=2592000");

    HttpResponse<String> me = send(HttpRequest.newBuilder(uri("/api/v1/me")));
    assertThat(me.statusCode()).isEqualTo(200);
    assertThat(me.body()).contains("\"email\":\"julia@example.com\"");
  }

  private HttpResponse<String> send(HttpRequest.Builder request) throws Exception {
    return http.send(request.build(), BodyHandlers.ofString());
  }

  private URI uri(String path) {
    return URI.create("http://localhost:" + port + path);
  }

  private String cookie(String name) {
    List<HttpCookie> all = cookies.getCookieStore().getCookies();
    return all.stream().filter(c -> c.getName().equals(name)).findFirst().orElseThrow().getValue();
  }

  private static String setCookie(HttpResponse<?> response, String name) {
    return response.headers().allValues("Set-Cookie").stream()
        .filter(header -> header.startsWith(name + "="))
        .findFirst()
        .orElse(null);
  }
}
