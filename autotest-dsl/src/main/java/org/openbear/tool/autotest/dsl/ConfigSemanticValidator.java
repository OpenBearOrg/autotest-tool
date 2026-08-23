package org.openbear.tool.autotest.dsl;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.openbear.tool.autotest.core.config.DatabaseConfig;
import org.openbear.tool.autotest.core.config.EnvironmentConfig;
import org.openbear.tool.autotest.core.config.MessagingConfig;
import org.openbear.tool.autotest.core.config.ProjectConfig;
import org.openbear.tool.autotest.core.config.SecretRef;
import org.openbear.tool.autotest.core.config.ServiceConfig;
import org.openbear.tool.autotest.core.util.ByteSizeParser;
import org.openbear.tool.autotest.core.util.DurationParser;

final class ConfigSemanticValidator {
  private ConfigSemanticValidator() {}

  static void validate(ProjectConfig project) {
    List<String> errors = new ArrayList<>();
    positive(project.getDefaults().getPolling().getTimeout(), "defaults.polling.timeout", errors);
    positive(project.getDefaults().getPolling().getInterval(), "defaults.polling.interval", errors);
    try {
      ByteSizeParser.parse(project.getReporting().getPayloadMaxSize());
    } catch (Exception e) {
      errors.add("reporting.payloadMaxSize: " + e.getMessage());
    }
    if (project.getExecution().getParallelism() < 1)
      errors.add("execution.parallelism must be >= 1");
    fail("Project configuration is invalid", errors);
  }

  static void validate(EnvironmentConfig environment) {
    List<String> errors = new ArrayList<>();
    for (Map.Entry<String, ServiceConfig> entry : environment.getServices().entrySet()) {
      String prefix = "services." + entry.getKey();
      ServiceConfig s = entry.getValue();
      uri(s.getBaseUrl(), prefix + ".baseUrl", errors);
      positive(s.getConnectTimeout(), prefix + ".connectTimeout", errors);
      positive(s.getRequestTimeout(), prefix + ".requestTimeout", errors);
    }
    for (Map.Entry<String, DatabaseConfig> entry : environment.getDatabases().entrySet()) {
      String prefix = "databases." + entry.getKey();
      DatabaseConfig d = entry.getValue();
      if (d.getDriver() == null || d.getDriver().isBlank())
        errors.add(prefix + ".driver is required");
      if (d.getJdbcUrl() == null || d.getJdbcUrl().isBlank())
        errors.add(prefix + ".jdbcUrl is required");
      secret(d.getUsername(), prefix + ".username", errors);
      secret(d.getPassword(), prefix + ".password", errors);
      positive(d.getConnectionTimeout(), prefix + ".connectionTimeout", errors);
      positive(d.getValidationTimeout(), prefix + ".validationTimeout", errors);
    }
    for (Map.Entry<String, MessagingConfig> entry : environment.getMessaging().entrySet()) {
      String prefix = "messaging." + entry.getKey();
      MessagingConfig m = entry.getValue();
      if (m.getType() == null || m.getType().isBlank()) errors.add(prefix + ".type is required");
      if (m.getBrokerUrl() == null || m.getBrokerUrl().isBlank())
        errors.add(prefix + ".brokerUrl is required");
      secret(m.getUsername(), prefix + ".username", errors);
      secret(m.getPassword(), prefix + ".password", errors);
      positive(m.getConnectTimeout(), prefix + ".connectTimeout", errors);
    }
    positive(
        environment.getDefaults().getPolling().getTimeout(), "defaults.polling.timeout", errors);
    positive(
        environment.getDefaults().getPolling().getInterval(), "defaults.polling.interval", errors);
    fail("Environment configuration is invalid", errors);
  }

  private static void secret(SecretRef secret, String path, List<String> errors) {
    if (secret != null && (secret.getSecret() == null || secret.getSecret().isBlank()))
      errors.add(path + ".secret must not be blank");
  }

  private static void uri(String value, String path, List<String> errors) {
    try {
      URI u = URI.create(value);
      if (u.getScheme() == null || u.getHost() == null)
        errors.add(path + " must be an absolute URL");
    } catch (Exception e) {
      errors.add(path + ": invalid URL: " + value);
    }
  }

  private static void positive(String value, String path, List<String> errors) {
    try {
      Duration d = DurationParser.parse(value);
      if (d.isZero() || d.isNegative()) errors.add(path + " must be > 0");
    } catch (Exception e) {
      errors.add(path + ": " + e.getMessage());
    }
  }

  private static void fail(String message, List<String> errors) {
    if (!errors.isEmpty()) throw new ValidationException(message, errors);
  }
}
