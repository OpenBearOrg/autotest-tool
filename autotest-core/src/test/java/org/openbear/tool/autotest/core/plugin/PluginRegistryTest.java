package org.openbear.tool.autotest.core.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openbear.tool.autotest.spi.SpiVersion;
import org.openbear.tool.autotest.spi.plugin.AutotestPlugin;
import org.openbear.tool.autotest.spi.plugin.PluginDescriptor;
import org.openbear.tool.autotest.spi.plugin.PluginRuntime;
import org.openbear.tool.autotest.spi.plugin.PluginRuntimeContext;
import org.openbear.tool.autotest.spi.resource.EnvironmentView;
import org.openbear.tool.autotest.spi.service.ResourceAccess;
import org.openbear.tool.autotest.spi.service.Secrets;

class PluginRegistryTest {
  @Test
  void rejectsDuplicateIdsAndUnsupportedSpiVersions() {
    PluginRegistry registry = new PluginRegistry();
    registry.register(plugin("one", SpiVersion.CURRENT, new ArrayList<>()));
    assertThrows(
        IllegalStateException.class,
        () -> registry.register(plugin("one", SpiVersion.CURRENT, new ArrayList<>())));
    assertThrows(
        IllegalStateException.class,
        () -> registry.register(plugin("two", "old", new ArrayList<>())));
  }

  @Test
  void closesRuntimesInReverseOrder() {
    List<String> closed = new ArrayList<>();
    PluginRegistry registry =
        new PluginRegistry()
            .register(plugin("one", SpiVersion.CURRENT, closed))
            .register(plugin("two", SpiVersion.CURRENT, closed));
    registry.open(context()).close();
    assertEquals(List.of("two", "one"), closed);
  }

  @Test
  void discoversCorePluginThroughServiceLoader() {
    assertEquals(
        List.of("autotest-core"),
        new ServiceLoaderPluginDiscovery()
            .discover().stream().map(plugin -> plugin.descriptor().id()).toList());
  }

  private static AutotestPlugin plugin(String id, String spiVersion, List<String> closed) {
    return new AutotestPlugin() {
      @Override
      public PluginDescriptor descriptor() {
        return new PluginDescriptor(id, id, "1", spiVersion);
      }

      @Override
      public PluginRuntime open(PluginRuntimeContext context) {
        return new PluginRuntime() {
          @Override
          public void close() {
            closed.add(id);
          }
        };
      }
    };
  }

  private static PluginRuntimeContext context() {
    EnvironmentView environment = () -> "test";
    ResourceAccess resources =
        new ResourceAccess() {
          @Override
          public String readText(String workspaceRelativePath) {
            return "";
          }

          @Override
          public byte[] readBytes(String workspaceRelativePath) {
            return new byte[0];
          }
        };
    Secrets secrets = reference -> java.util.Optional.empty();
    return new PluginRuntimeContext(environment, resources, secrets, Clock.systemUTC());
  }
}
