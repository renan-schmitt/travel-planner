package com.travelplanner.shared.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import org.springframework.boot.json.JsonWriter;
import org.springframework.boot.logging.structured.StructuredLogFormatter;

/**
 * Log em JSON, uma linha por evento, no formato que o Cloud Logging lê do stdout do Cloud Run:
 * {@code severity} vira o nível do log e {@code message} o texto. A pilha de uma exceção vai junto
 * da mensagem, como o Error Reporting espera.
 */
public class GcpLogFormatter implements StructuredLogFormatter<ILoggingEvent> {

  private final JsonWriter<ILoggingEvent> writer =
      JsonWriter.<ILoggingEvent>of(
              members -> {
                members.add("time", ILoggingEvent::getInstant);
                members.add("severity", event -> severity(event.getLevel()));
                members.add("message", GcpLogFormatter::message);
                members.add("logger", ILoggingEvent::getLoggerName);
                members.add("thread", ILoggingEvent::getThreadName);
              })
          .withNewLineAtEnd();

  @Override
  public String format(ILoggingEvent event) {
    return writer.writeToString(event);
  }

  private static String message(ILoggingEvent event) {
    if (event.getThrowableProxy() == null) {
      return event.getFormattedMessage();
    }
    return event.getFormattedMessage()
        + "\n"
        + ThrowableProxyUtil.asString(event.getThrowableProxy());
  }

  /** Níveis do Logback → {@code LogSeverity} do Cloud Logging. */
  static String severity(Level level) {
    return switch (level.toInt()) {
      case Level.ERROR_INT -> "ERROR";
      case Level.WARN_INT -> "WARNING";
      case Level.INFO_INT -> "INFO";
      default -> "DEBUG";
    };
  }
}
