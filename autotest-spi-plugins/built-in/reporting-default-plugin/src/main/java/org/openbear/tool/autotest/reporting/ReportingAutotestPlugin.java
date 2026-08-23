package org.openbear.tool.autotest.reporting;

import java.util.List;
import org.openbear.tool.autotest.spi.SpiVersion;
import org.openbear.tool.autotest.spi.plugin.AutotestPlugin;
import org.openbear.tool.autotest.spi.plugin.CapabilityDescriptor;
import org.openbear.tool.autotest.spi.plugin.CapabilityProvider;
import org.openbear.tool.autotest.spi.plugin.PluginDescriptor;

/** Public-SPI descriptor for the default reporting plugin. */
public final class ReportingAutotestPlugin implements AutotestPlugin {
  @Override
  public PluginDescriptor descriptor() {
    return new PluginDescriptor(
        "reporting-default", "Default HTML Reporting", "1.0.0", SpiVersion.CURRENT);
  }

  @Override
  public List<? extends CapabilityProvider<?>> capabilityProviders() {
    return List.of(
        new CapabilityProvider<DefaultRunReportWriter>() {
          @Override
          public CapabilityDescriptor<DefaultRunReportWriter> descriptor() {
            return new CapabilityDescriptor<>(
                "default-run-report-writer", DefaultRunReportWriter.class);
          }

          @Override
          public DefaultRunReportWriter open(
              org.openbear.tool.autotest.spi.plugin.PluginRuntimeContext context) {
            return new DefaultRunReportWriter();
          }
        },
        new CapabilityProvider<DefaultComparisonReportWriter>() {
          @Override
          public CapabilityDescriptor<DefaultComparisonReportWriter> descriptor() {
            return new CapabilityDescriptor<>(
                "default-comparison-report-writer", DefaultComparisonReportWriter.class);
          }

          @Override
          public DefaultComparisonReportWriter open(
              org.openbear.tool.autotest.spi.plugin.PluginRuntimeContext context) {
            return new DefaultComparisonReportWriter();
          }
        });
  }
}
