package org.openbear.tool.autotest.core.domain;

import java.util.List;
import java.util.Optional;
import org.openbear.tool.autotest.spi.resource.EnvironmentView;
import org.openbear.tool.autotest.spi.resource.PluginResource;

/** Public-SPI view over the immutable environment compiled by the DSL module. */
public final class CompiledEnvironmentView implements EnvironmentView {
  private final CompiledEnvironment environment;

  public CompiledEnvironmentView(CompiledEnvironment environment) {
    this.environment = java.util.Objects.requireNonNull(environment, "environment");
  }

  @Override
  public String name() {
    return environment.name();
  }

  @Override
  public Optional<org.openbear.tool.autotest.spi.resource.DatabaseResource> database(String name) {
    return environment
        .database(name)
        .map(
            resource ->
                new org.openbear.tool.autotest.spi.resource.DatabaseResource(
                    resource.name(),
                    resource.driver(),
                    resource.jdbcUrl(),
                    resource.username() == null ? null : resource.username().value(),
                    resource.password() == null ? null : resource.password().value(),
                    resource.maximumPoolSize(),
                    resource.connectionTimeout(),
                    resource.validationTimeout(),
                    resource.allowWrites(),
                    resource.options()));
  }

  @Override
  public List<org.openbear.tool.autotest.spi.resource.DatabaseResource> databases() {
    return environment.databaseResources().stream()
        .map(
            resource ->
                new org.openbear.tool.autotest.spi.resource.DatabaseResource(
                    resource.name(),
                    resource.driver(),
                    resource.jdbcUrl(),
                    resource.username() == null ? null : resource.username().value(),
                    resource.password() == null ? null : resource.password().value(),
                    resource.maximumPoolSize(),
                    resource.connectionTimeout(),
                    resource.validationTimeout(),
                    resource.allowWrites(),
                    resource.options()))
        .toList();
  }

  @Override
  public Optional<org.openbear.tool.autotest.spi.resource.ServiceResource> service(String name) {
    return environment
        .service(name)
        .map(
            resource ->
                new org.openbear.tool.autotest.spi.resource.ServiceResource(
                    resource.name(),
                    resource.baseUri().toString(),
                    resource.defaultHeaders(),
                    resource.connectTimeout(),
                    resource.requestTimeout(),
                    resource.healthPath(),
                    resource.safeRetryAttempts()));
  }

  @Override
  public Optional<org.openbear.tool.autotest.spi.resource.MessagingResource> messaging(
      String name) {
    return environment
        .messaging(name)
        .map(
            resource ->
                new org.openbear.tool.autotest.spi.resource.MessagingResource(
                    resource.name(),
                    resource.provider(),
                    resource.brokerUri().toString(),
                    resource.username() == null ? null : resource.username().value(),
                    resource.password() == null ? null : resource.password().value(),
                    resource.connectTimeout()));
  }

  @Override
  public <T extends PluginResource> Optional<T> resource(
      String type, String name, Class<T> resourceType) {
    return environment.resource(type, name, resourceType);
  }

  @Override
  public <T extends PluginResource> List<T> resources(String type, Class<T> resourceType) {
    return environment.resources(type, resourceType);
  }
}
