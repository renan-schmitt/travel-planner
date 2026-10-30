package com.travelplanner.shared.logging;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class GcpLogFormatterTest {

  private final GcpLogFormatter formatter = new GcpLogFormatter();
  private final LoggerContext context = new LoggerContext();
  private final JsonMapper json = JsonMapper.builder().build();

  @Test
  void writesOneJsonLineWithSeverityAndMessage() {
    LoggingEvent event =
        new LoggingEvent(
            "fqcn",
            context.getLogger("com.example.Foo"),
            Level.WARN,
            "olá {}",
            null,
            args("mundo"));

    String line = formatter.format(event);

    assertThat(line).endsWith("\n").doesNotContain("\n{");
    JsonNode node = json.readTree(line);
    assertThat(node.get("severity").asString()).isEqualTo("WARNING");
    assertThat(node.get("message").asString()).isEqualTo("olá mundo");
    assertThat(node.get("logger").asString()).isEqualTo("com.example.Foo");
    assertThat(node.has("time")).isTrue();
  }

  @Test
  void appendsStackTraceToMessage() {
    LoggingEvent event =
        new LoggingEvent(
            "fqcn",
            context.getLogger("com.example.Foo"),
            Level.ERROR,
            "falhou",
            new IllegalStateException("boom"),
            null);

    JsonNode node = json.readTree(formatter.format(event));

    assertThat(node.get("severity").asString()).isEqualTo("ERROR");
    assertThat(node.get("message").asString())
        .startsWith("falhou\n")
        .contains("java.lang.IllegalStateException: boom")
        .contains("at com.travelplanner.shared.logging.GcpLogFormatterTest");
  }

  @Test
  void mapsLevelsToCloudLoggingSeverity() {
    assertThat(GcpLogFormatter.severity(Level.ERROR)).isEqualTo("ERROR");
    assertThat(GcpLogFormatter.severity(Level.INFO)).isEqualTo("INFO");
    assertThat(GcpLogFormatter.severity(Level.DEBUG)).isEqualTo("DEBUG");
    assertThat(GcpLogFormatter.severity(Level.TRACE)).isEqualTo("DEBUG");
  }

  private static Object[] args(Object... args) {
    return args;
  }
}
