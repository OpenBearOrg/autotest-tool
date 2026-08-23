package org.openbear.tool.autotest.core.domain;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

public record DatabaseResource(
    ResourceId id,
    String driver,
    String jdbcUrl,
    SecretReference username,
    SecretReference password,
    int maximumPoolSize,
    Duration connectionTimeout,
    Duration validationTimeout,
    boolean allowWrites,
    Map<String, Object> options) {
  public DatabaseResource {
    Objects.requireNonNull(id, "id");
    driver = IdValue.requireNonBlank(driver, "driver");
    jdbcUrl = IdValue.requireNonBlank(jdbcUrl, "jdbcUrl");
    if (maximumPoolSize < 1) throw new IllegalArgumentException("maximumPoolSize must be >= 1");
    connectionTimeout = Objects.requireNonNull(connectionTimeout, "connectionTimeout");
    validationTimeout = Objects.requireNonNull(validationTimeout, "validationTimeout");
    options = ImmutableValues.map(options);
  }

  public String name() {
    return id.value();
  }
}
