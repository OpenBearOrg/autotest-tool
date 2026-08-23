package org.openbear.tool.autotest.core.domain;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

public record ServiceResource(
    ResourceId id,
    URI baseUri,
    Map<String, String> defaultHeaders,
    Duration connectTimeout,
    Duration requestTimeout,
    String healthPath,
    int safeRetryAttempts) {
  public ServiceResource {
    Objects.requireNonNull(id, "id");
    baseUri = Objects.requireNonNull(baseUri, "baseUri");
    defaultHeaders = ImmutableValues.strings(defaultHeaders);
    connectTimeout = Objects.requireNonNull(connectTimeout, "connectTimeout");
    requestTimeout = Objects.requireNonNull(requestTimeout, "requestTimeout");
    healthPath = IdValue.requireNonBlank(healthPath, "healthPath");
    if (connectTimeout.isNegative() || connectTimeout.isZero())
      throw new IllegalArgumentException("connectTimeout must be > 0");
    if (requestTimeout.isNegative() || requestTimeout.isZero())
      throw new IllegalArgumentException("requestTimeout must be > 0");
    if (safeRetryAttempts < 1) throw new IllegalArgumentException("safeRetryAttempts must be >= 1");
  }

  public String name() {
    return id.value();
  }
}
