package org.openbear.tool.autotest.spi.plugin;

import java.time.Clock;
import java.util.List;
import java.util.Objects;
import org.openbear.tool.autotest.spi.resource.EnvironmentView;
import org.openbear.tool.autotest.spi.service.ResourceAccess;
import org.openbear.tool.autotest.spi.service.Secrets;

public record PluginRuntimeContext(
    EnvironmentView environment,
    ResourceAccess resources,
    Secrets secrets,
    Clock clock,
    Capabilities capabilities) {
  public PluginRuntimeContext(
      EnvironmentView environment, ResourceAccess resources, Secrets secrets, Clock clock) {
    this(
        environment,
        resources,
        secrets,
        clock,
        new Capabilities() {
          @Override
          public <T> List<T> all(Class<T> type) {
            return List.of();
          }
        });
  }

  public PluginRuntimeContext {
    Objects.requireNonNull(environment, "environment");
    Objects.requireNonNull(resources, "resources");
    Objects.requireNonNull(secrets, "secrets");
    Objects.requireNonNull(clock, "clock");
    Objects.requireNonNull(capabilities, "capabilities");
  }
}
