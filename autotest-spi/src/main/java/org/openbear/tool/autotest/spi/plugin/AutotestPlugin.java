package org.openbear.tool.autotest.spi.plugin;

import java.util.Collection;
import java.util.List;
import org.openbear.tool.autotest.spi.resource.ResourceTypeProvider;
import org.openbear.tool.autotest.spi.step.StepTypeProvider;

/**
 * Contributes step types, resources, capabilities, validation, and runtime behavior to AutoTest.
 * Implementations are discovered through {@link java.util.ServiceLoader} and must be stateless or
 * thread-safe. Runtime resources returned by {@link #open(PluginRuntimeContext)} are closed in
 * reverse plugin order after a run.
 *
 * @since 1.0
 */
@FunctionalInterface
public interface AutotestPlugin {
  PluginDescriptor descriptor();

  default Collection<? extends StepTypeProvider<?, ?>> stepTypes() {
    return List.of();
  }

  default Collection<? extends ResourceTypeProvider<?, ?>> resourceTypes() {
    return List.of();
  }

  default Collection<? extends ValidationProvider> validators() {
    return List.of();
  }

  default Collection<? extends CapabilityProvider<?>> capabilityProviders() {
    return List.of();
  }

  default PluginRuntime open(PluginRuntimeContext context) {
    return PluginRuntime.empty();
  }
}
