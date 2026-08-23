package org.openbear.tool.autotest.spi.resource;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

public record DatabaseResource(
    String name,
    String driver,
    String jdbcUrl,
    String username,
    String password,
    int maximumPoolSize,
    Duration connectionTimeout,
    Duration validationTimeout,
    boolean allowWrites,
    Map<String, Object> options) {
  public DatabaseResource {
    name = requireText(name, "name");
    driver = requireText(driver, "driver");
    jdbcUrl = requireText(jdbcUrl, "jdbcUrl");
    if (maximumPoolSize < 1) throw new IllegalArgumentException("maximumPoolSize must be >= 1");
    connectionTimeout = Objects.requireNonNull(connectionTimeout, "connectionTimeout");
    validationTimeout = Objects.requireNonNull(validationTimeout, "validationTimeout");
    options = options == null ? Map.of() : Map.copyOf(options);
  }

  private static String requireText(String value, String name) {
    String text = Objects.requireNonNull(value, name).trim();
    if (text.isEmpty()) throw new IllegalArgumentException(name + " must not be blank");
    return text;
  }
}
