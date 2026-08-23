package org.openbear.tool.autotest.core.plugin;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.openbear.tool.autotest.spi.resource.ResourceTypeProvider;

/** Registry of plugin-contributed environment resource types. */
public final class ResourceTypeRegistry {
  private final Map<String, ResourceTypeProvider<?, ?>> providers = new LinkedHashMap<>();

  public synchronized ResourceTypeRegistry register(ResourceTypeProvider<?, ?> provider) {
    ResourceTypeProvider<?, ?> value = Objects.requireNonNull(provider, "provider");
    String type = Objects.requireNonNull(value.type(), "resource type");
    if (type.isBlank()) throw new IllegalArgumentException("Resource type must not be blank");
    if (providers.putIfAbsent(type, value) != null)
      throw new IllegalStateException("Duplicate resource type: " + type);
    return this;
  }

  public synchronized ResourceTypeRegistry registerAll(
      Collection<? extends ResourceTypeProvider<?, ?>> values) {
    for (ResourceTypeProvider<?, ?> provider : values) register(provider);
    return this;
  }

  public synchronized Optional<ResourceTypeProvider<?, ?>> find(String type) {
    return Optional.ofNullable(providers.get(type));
  }

  public synchronized ResourceTypeProvider<?, ?> require(String type) {
    return find(type)
        .orElseThrow(() -> new IllegalArgumentException("Unknown resource type: " + type));
  }

  public synchronized Collection<ResourceTypeProvider<?, ?>> providers() {
    return java.util.List.copyOf(providers.values());
  }
}
