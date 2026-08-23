package org.openbear.tool.autotest.testkit;

import java.util.Collection;
import java.util.List;
import org.openbear.tool.autotest.core.plugin.PluginRegistry;
import org.openbear.tool.autotest.spi.SpiVersion;
import org.openbear.tool.autotest.spi.plugin.AutotestPlugin;
import org.openbear.tool.autotest.spi.plugin.CapabilityDescriptor;
import org.openbear.tool.autotest.spi.plugin.CapabilityProvider;
import org.openbear.tool.autotest.spi.plugin.PluginDescriptor;
import org.openbear.tool.autotest.spi.plugin.PluginRuntimeContext;

/** Small fixtures for embedding tests that need predictable plugins and capabilities. */
public final class TestPlugins {
  private TestPlugins() {}

  public static PluginRegistry registry(AutotestPlugin... plugins) {
    PluginRegistry registry = new PluginRegistry();
    if (plugins != null) registry.registerAll(List.of(plugins));
    return registry;
  }

  public static AutotestPlugin plugin(
      String id, String name, String version, CapabilityProvider<?>... capabilities) {
    return new SimplePlugin(
        id, name, version, capabilities == null ? List.of() : List.of(capabilities));
  }

  public static <T> CapabilityProvider<T> capability(String id, Class<T> type, T value) {
    return new FixedCapability<>(id, type, value);
  }

  private static final class SimplePlugin implements AutotestPlugin {
    private final String id;
    private final String name;
    private final String version;
    private final Collection<? extends CapabilityProvider<?>> capabilities;

    private SimplePlugin(
        String id,
        String name,
        String version,
        Collection<? extends CapabilityProvider<?>> capabilities) {
      this.id = id;
      this.name = name;
      this.version = version;
      this.capabilities = capabilities;
    }

    @Override
    public PluginDescriptor descriptor() {
      return new PluginDescriptor(id, name, version, SpiVersion.CURRENT);
    }

    @Override
    public Collection<? extends CapabilityProvider<?>> capabilityProviders() {
      return capabilities;
    }
  }

  private static final class FixedCapability<T> implements CapabilityProvider<T> {
    private final CapabilityDescriptor descriptor;
    private final T value;

    private FixedCapability(String id, Class<T> type, T value) {
      this.descriptor = new CapabilityDescriptor(id, type);
      this.value = value;
    }

    @Override
    public CapabilityDescriptor descriptor() {
      return descriptor;
    }

    @Override
    public T open(PluginRuntimeContext context) {
      return value;
    }
  }
}
