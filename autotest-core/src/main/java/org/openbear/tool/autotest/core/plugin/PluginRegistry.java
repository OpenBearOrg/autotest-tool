package org.openbear.tool.autotest.core.plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.openbear.tool.autotest.spi.SpiVersion;
import org.openbear.tool.autotest.spi.doctor.DoctorCheck;
import org.openbear.tool.autotest.spi.plugin.AutotestPlugin;
import org.openbear.tool.autotest.spi.plugin.Capabilities;
import org.openbear.tool.autotest.spi.plugin.CapabilityProvider;
import org.openbear.tool.autotest.spi.plugin.PluginDescriptor;
import org.openbear.tool.autotest.spi.plugin.PluginRuntime;
import org.openbear.tool.autotest.spi.plugin.PluginRuntimeContext;
import org.openbear.tool.autotest.spi.resource.ResourceTypeProvider;
import org.openbear.tool.autotest.spi.step.ExecutableStep;
import org.openbear.tool.autotest.spi.step.StepHandler;

/** Validated registry and lifecycle owner for the public plugin SPI. */
public final class PluginRegistry implements AutoCloseable {
  private final Map<String, AutotestPlugin> plugins = new LinkedHashMap<>();
  private final Map<String, ResourceTypeProvider<?, ?>> resources = new LinkedHashMap<>();
  private final Map<String, CapabilityProvider<?>> capabilityProviders = new LinkedHashMap<>();
  private final Map<Class<?>, StepHandler<?>> handlers = new LinkedHashMap<>();
  private final Map<Class<?>, List<Object>> capabilityValues = new LinkedHashMap<>();
  private final List<PluginRuntime> runtimes = new ArrayList<>();
  private final List<AutoCloseable> closeables = new ArrayList<>();
  private final StepTypeRegistry stepTypes = new StepTypeRegistry();
  private final ResourceTypeRegistry resourceTypes = new ResourceTypeRegistry();
  private Capabilities capabilities =
      new Capabilities() {
        @Override
        public <T> List<T> all(Class<T> type) {
          return List.of();
        }
      };

  public synchronized PluginRegistry register(AutotestPlugin plugin) {
    AutotestPlugin value = Objects.requireNonNull(plugin, "plugin");
    PluginDescriptor descriptor = Objects.requireNonNull(value.descriptor(), "plugin descriptor");
    if (!SpiVersion.CURRENT.equals(descriptor.spiVersion()))
      throw new IllegalStateException(
          "Unsupported SPI version for plugin " + descriptor.id() + ": " + descriptor.spiVersion());
    if (plugins.putIfAbsent(descriptor.id(), value) != null)
      throw new IllegalStateException("Duplicate plugin id: " + descriptor.id());
    try {
      List<ResourceTypeProvider<?, ?>> resourceProviders = List.copyOf(value.resourceTypes());
      java.util.Set<String> resourceNames = new java.util.LinkedHashSet<>();
      for (ResourceTypeProvider<?, ?> provider : resourceProviders) {
        String type = Objects.requireNonNull(provider.type(), "resource type");
        if (!resourceNames.add(type) || resources.containsKey(type))
          throw new IllegalStateException("Duplicate resource type: " + type);
      }
      List<CapabilityProvider<?>> providers = List.copyOf(value.capabilityProviders());
      java.util.Set<String> capabilityIds = new java.util.LinkedHashSet<>();
      for (CapabilityProvider<?> provider : providers) {
        String id = provider.descriptor().id();
        if (!capabilityIds.add(id) || capabilityProviders.containsKey(id))
          throw new IllegalStateException("Duplicate capability id: " + id);
      }
      stepTypes.registerAll(value.stepTypes());
      resourceTypes.registerAll(resourceProviders);
      for (ResourceTypeProvider<?, ?> provider : resourceProviders)
        resources.put(provider.type(), provider);
      for (CapabilityProvider<?> provider : providers)
        capabilityProviders.put(provider.descriptor().id(), provider);
      rebuildCapabilities();
      return this;
    } catch (RuntimeException failure) {
      plugins.remove(descriptor.id());
      throw failure;
    }
  }

  public synchronized PluginRegistry registerAll(Iterable<? extends AutotestPlugin> values) {
    for (AutotestPlugin plugin : values) register(plugin);
    return this;
  }

  public synchronized Optional<AutotestPlugin> plugin(String id) {
    return Optional.ofNullable(plugins.get(id));
  }

  public synchronized Collection<AutotestPlugin> plugins() {
    return List.copyOf(plugins.values());
  }

  public synchronized StepTypeRegistry stepTypes() {
    return stepTypes;
  }

  public synchronized Optional<ResourceTypeProvider<?, ?>> resourceType(String type) {
    return Optional.ofNullable(resources.get(type));
  }

  public synchronized ResourceTypeRegistry resourceTypes() {
    return resourceTypes;
  }

  public synchronized Capabilities capabilities() {
    return capabilities;
  }

  public synchronized PluginRegistry open(PluginRuntimeContext context) {
    Objects.requireNonNull(context, "context");
    if (!runtimes.isEmpty()) throw new IllegalStateException("Plugin registry is already open");
    List<PluginRuntime> opened = new ArrayList<>();
    try {
      PluginRuntimeContext runtimeContext =
          new PluginRuntimeContext(
              context.environment(),
              context.resources(),
              context.secrets(),
              context.clock(),
              capabilities);
      capabilityValues.clear();
      for (CapabilityProvider<?> provider : capabilityProviders.values()) {
        Object value = Objects.requireNonNull(provider.open(runtimeContext), "capability");
        capabilityValues
            .computeIfAbsent(provider.descriptor().type(), ignored -> new ArrayList<>())
            .add(value);
      }
      for (AutotestPlugin plugin : plugins.values()) {
        PluginRuntime runtime =
            Objects.requireNonNull(plugin.open(runtimeContext), "plugin runtime");
        opened.add(runtime);
        for (StepHandler<?> handler : runtime.stepHandlers()) {
          Class<?> type = Objects.requireNonNull(handler.stepType(), "step handler type");
          if (handlers.putIfAbsent(type, handler) != null)
            throw new IllegalStateException("Duplicate step handler type: " + type.getName());
        }
      }
      runtimes.addAll(opened);
      return this;
    } catch (RuntimeException | Error failure) {
      closeReverse(opened, failure);
      handlers.clear();
      capabilityValues.clear();
      throw failure;
    }
  }

  @SuppressWarnings("unchecked")
  public synchronized <S extends ExecutableStep> Optional<StepHandler<S>> handler(Class<S> type) {
    return Optional.ofNullable((StepHandler<S>) handlers.get(type));
  }

  public synchronized Collection<DoctorCheck> doctorChecks() {
    List<DoctorCheck> checks = new ArrayList<>();
    for (PluginRuntime runtime : runtimes) checks.addAll(runtime.doctorChecks());
    return List.copyOf(checks);
  }

  /** Registers an external loading resource that is owned by this registry lifecycle. */
  public synchronized PluginRegistry addCloseable(AutoCloseable closeable) {
    closeables.add(Objects.requireNonNull(closeable, "closeable"));
    return this;
  }

  @Override
  public synchronized void close() {
    RuntimeException failure = null;
    for (int i = runtimes.size() - 1; i >= 0; i--) {
      try {
        runtimes.get(i).close();
      } catch (Exception e) {
        RuntimeException wrapped =
            e instanceof RuntimeException runtime
                ? runtime
                : new IllegalStateException("Failed to close plugin runtime", e);
        if (failure == null) failure = wrapped;
        else failure.addSuppressed(wrapped);
      }
    }
    runtimes.clear();
    handlers.clear();
    for (int i = closeables.size() - 1; i >= 0; i--) {
      try {
        closeables.get(i).close();
      } catch (Exception e) {
        RuntimeException wrapped =
            e instanceof RuntimeException runtime
                ? runtime
                : new IllegalStateException("Failed to close plugin loading resource", e);
        if (failure == null) failure = wrapped;
        else failure.addSuppressed(wrapped);
      }
    }
    closeables.clear();
    if (failure != null) throw failure;
  }

  private void rebuildCapabilities() {
    capabilities =
        new Capabilities() {
          @Override
          public <T> List<T> all(Class<T> type) {
            List<T> matches = new ArrayList<>();
            for (Object value : capabilityValues.getOrDefault(type, List.of()))
              matches.add(type.cast(value));
            return List.copyOf(matches);
          }
        };
  }

  private static void closeReverse(List<PluginRuntime> values, Throwable failure) {
    for (int i = values.size() - 1; i >= 0; i--) {
      try {
        values.get(i).close();
      } catch (Exception closeFailure) {
        failure.addSuppressed(closeFailure);
      }
    }
  }
}
