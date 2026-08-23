package org.openbear.tool.autotest.core.plugin;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import org.openbear.tool.autotest.spi.plugin.AutotestPlugin;

public final class ServiceLoaderPluginDiscovery {
  private final ClassLoader classLoader;

  public ServiceLoaderPluginDiscovery() {
    this(Thread.currentThread().getContextClassLoader());
  }

  public ServiceLoaderPluginDiscovery(ClassLoader classLoader) {
    this.classLoader = Objects.requireNonNull(classLoader, "classLoader");
  }

  public List<AutotestPlugin> discover() {
    return ServiceLoader.load(AutotestPlugin.class, classLoader).stream()
        .map(ServiceLoader.Provider::get)
        .sorted(Comparator.comparing(plugin -> plugin.descriptor().id()))
        .toList();
  }
}
