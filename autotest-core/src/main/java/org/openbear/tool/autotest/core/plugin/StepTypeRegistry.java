package org.openbear.tool.autotest.core.plugin;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.openbear.tool.autotest.spi.step.StepTypeProvider;

/** Registry of DSL step providers contributed by loaded plugins. */
public final class StepTypeRegistry {
  private final Map<String, StepTypeProvider<?, ?>> providers = new LinkedHashMap<>();

  public synchronized StepTypeRegistry register(StepTypeProvider<?, ?> provider) {
    StepTypeProvider<?, ?> value = Objects.requireNonNull(provider, "provider");
    String type = Objects.requireNonNull(value.type(), "step type");
    if (type.isBlank()) throw new IllegalArgumentException("step type must not be blank");
    if (providers.putIfAbsent(type, value) != null)
      throw new IllegalStateException("Duplicate step type: " + type);
    return this;
  }

  public synchronized StepTypeRegistry registerAll(
      Collection<? extends StepTypeProvider<?, ?>> values) {
    java.util.Set<String> seen = new java.util.LinkedHashSet<>();
    for (StepTypeProvider<?, ?> provider : values) {
      String type = Objects.requireNonNull(provider, "provider").type();
      if (!seen.add(type) || providers.containsKey(type))
        throw new IllegalStateException("Duplicate step type: " + type);
    }
    for (StepTypeProvider<?, ?> provider : values) register(provider);
    return this;
  }

  public synchronized Optional<StepTypeProvider<?, ?>> find(String type) {
    return Optional.ofNullable(providers.get(type));
  }

  public synchronized StepTypeProvider<?, ?> require(String type) {
    return find(type).orElseThrow(() -> new IllegalArgumentException("Unknown step type: " + type));
  }

  public synchronized List<String> types() {
    return List.copyOf(providers.keySet());
  }

  public synchronized Collection<StepTypeProvider<?, ?>> providers() {
    return List.copyOf(providers.values());
  }
}
