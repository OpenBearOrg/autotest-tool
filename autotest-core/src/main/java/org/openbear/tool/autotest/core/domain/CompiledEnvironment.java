package org.openbear.tool.autotest.core.domain;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.openbear.tool.autotest.spi.resource.PluginResource;

public record CompiledEnvironment(
    EnvironmentId id,
    Map<String, ServiceResource> services,
    Map<String, DatabaseResource> databases,
    Map<String, MessagingResource> messaging,
    boolean databaseWritesAllowed,
    PollingSettings defaultPolling,
    Map<String, List<PluginResource>> pluginResources) {
  public CompiledEnvironment(
      EnvironmentId id,
      Map<String, ServiceResource> services,
      Map<String, DatabaseResource> databases,
      Map<String, MessagingResource> messaging,
      boolean databaseWritesAllowed,
      PollingSettings defaultPolling) {
    this(id, services, databases, messaging, databaseWritesAllowed, defaultPolling, Map.of());
  }

  public CompiledEnvironment {
    Objects.requireNonNull(id, "id");
    services = copyResources(services);
    databases = copyResources(databases);
    messaging = copyResources(messaging);
    defaultPolling = Objects.requireNonNull(defaultPolling, "defaultPolling");
    pluginResources = copyPluginResources(pluginResources);
  }

  public String name() {
    return id.value();
  }

  public Optional<ServiceResource> service(String name) {
    return Optional.ofNullable(services.get(name));
  }

  public Optional<DatabaseResource> database(String name) {
    return Optional.ofNullable(databases.get(name));
  }

  public Optional<MessagingResource> messaging(String name) {
    return Optional.ofNullable(messaging.get(name));
  }

  public <T extends PluginResource> Optional<T> resource(
      String type, String name, Class<T> resourceType) {
    return resources(type, resourceType).stream()
        .filter(resource -> resource.name().equals(name))
        .findFirst();
  }

  public <T extends PluginResource> List<T> resources(String type, Class<T> resourceType) {
    return pluginResources.getOrDefault(type, List.of()).stream().map(resourceType::cast).toList();
  }

  public List<ServiceResource> serviceResources() {
    return List.copyOf(services.values());
  }

  public List<DatabaseResource> databaseResources() {
    return List.copyOf(databases.values());
  }

  public List<MessagingResource> messagingResources() {
    return List.copyOf(messaging.values());
  }

  private static <T> Map<String, T> copyResources(Map<String, T> resources) {
    if (resources == null || resources.isEmpty()) return Map.of();
    return Collections.unmodifiableMap(new LinkedHashMap<>(resources));
  }

  private static Map<String, List<PluginResource>> copyPluginResources(
      Map<String, List<PluginResource>> resources) {
    if (resources == null || resources.isEmpty()) return Map.of();
    Map<String, List<PluginResource>> copy = new LinkedHashMap<>();
    resources.forEach((type, values) -> copy.put(type, List.copyOf(values)));
    return Collections.unmodifiableMap(copy);
  }
}
