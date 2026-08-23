package org.openbear.tool.autotest.spi.resource;

import java.util.List;
import java.util.Optional;

@FunctionalInterface
public interface EnvironmentView {
  String name();

  default Optional<DatabaseResource> database(String name) {
    return Optional.empty();
  }

  default List<DatabaseResource> databases() {
    return List.of();
  }

  /** Returns a compiled HTTP service definition when the named service is configured. */
  default Optional<ServiceResource> service(String name) {
    return Optional.empty();
  }

  /** Returns a compiled messaging connection definition when the named connection is configured. */
  default Optional<MessagingResource> messaging(String name) {
    return Optional.empty();
  }

  default <T extends PluginResource> Optional<T> resource(
      String type, String name, Class<T> resourceType) {
    return resources(type, resourceType).stream()
        .filter(resource -> resource.name().equals(name))
        .findFirst();
  }

  default <T extends PluginResource> List<T> resources(String type, Class<T> resourceType) {
    return List.of();
  }
}
