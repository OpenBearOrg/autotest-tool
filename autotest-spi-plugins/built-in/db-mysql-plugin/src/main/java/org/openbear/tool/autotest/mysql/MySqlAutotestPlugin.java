package org.openbear.tool.autotest.mysql;

import java.util.List;
import org.openbear.tool.autotest.spi.SpiVersion;
import org.openbear.tool.autotest.spi.plugin.AutotestPlugin;
import org.openbear.tool.autotest.spi.plugin.CapabilityDescriptor;
import org.openbear.tool.autotest.spi.plugin.CapabilityProvider;
import org.openbear.tool.autotest.spi.plugin.PluginDescriptor;
import org.openbear.tool.autotest.spi.plugin.PluginRuntimeContext;

/** Public-SPI descriptor for the MySQL plugin during the JDBC migration. */
public final class MySqlAutotestPlugin implements AutotestPlugin {
  @Override
  public PluginDescriptor descriptor() {
    return new PluginDescriptor("mysql", "MySQL JDBC", "1.0.0", SpiVersion.CURRENT);
  }

  @Override
  public List<? extends CapabilityProvider<?>> capabilityProviders() {
    return List.of(
        new CapabilityProvider<org.openbear.tool.autotest.jdbc.spi.JdbcDriverProvider>() {
          @Override
          public CapabilityDescriptor<org.openbear.tool.autotest.jdbc.spi.JdbcDriverProvider>
              descriptor() {
            return new CapabilityDescriptor<>(
                "mysql-jdbc-driver", org.openbear.tool.autotest.jdbc.spi.JdbcDriverProvider.class);
          }

          @Override
          public org.openbear.tool.autotest.jdbc.spi.JdbcDriverProvider open(
              PluginRuntimeContext context) {
            return new MySqlJdbcDriverProvider();
          }
        });
  }
}
