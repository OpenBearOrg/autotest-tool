package org.openbear.tool.autotest.spi.plugin;

public interface CapabilityProvider<T> {
  CapabilityDescriptor<T> descriptor();

  T open(PluginRuntimeContext context);
}
