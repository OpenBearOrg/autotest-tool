package org.openbear.tool.autotest.core.secret;

import java.util.Optional;
import org.openbear.tool.autotest.spi.service.Secrets;

/** Resolves literal and environment-backed secret references for public plugin runtimes. */
public final class EnvironmentSecretProvider implements Secrets {
  @Override
  public Optional<String> resolve(String key) {
    if (key == null || key.isBlank()) return Optional.empty();
    String environmentName = environmentName(key);
    if (environmentName != null)
      return environmentName.isBlank()
          ? Optional.empty()
          : Optional.ofNullable(System.getenv(environmentName));
    String environmentValue = System.getenv(key);
    return Optional.of(environmentValue == null ? key : environmentValue);
  }

  private static String environmentName(String key) {
    if (!key.startsWith("${") || !key.endsWith("}")) return null;
    String inner = key.substring(2, key.length() - 1);
    return inner.startsWith("ENV:") ? inner.substring(4) : inner;
  }
}
