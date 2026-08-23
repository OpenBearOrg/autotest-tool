package org.openbear.tool.autotest.core.domain;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

public record MessagingResource(
    ResourceId id,
    String provider,
    URI brokerUri,
    SecretReference username,
    SecretReference password,
    Duration connectTimeout,
    Map<String, Object> options) {
  public MessagingResource {
    Objects.requireNonNull(id, "id");
    provider = IdValue.requireNonBlank(provider, "provider");
    brokerUri = Objects.requireNonNull(brokerUri, "brokerUri");
    connectTimeout = Objects.requireNonNull(connectTimeout, "connectTimeout");
    if (connectTimeout.isNegative() || connectTimeout.isZero())
      throw new IllegalArgumentException("connectTimeout must be > 0");
    options = ImmutableValues.map(options);
  }

  public String name() {
    return id.value();
  }
}
