package org.openbear.tool.autotest.http;

import java.util.Collection;
import java.util.List;
import org.openbear.tool.autotest.spi.SpiVersion;
import org.openbear.tool.autotest.spi.plugin.AutotestPlugin;
import org.openbear.tool.autotest.spi.plugin.PluginDescriptor;
import org.openbear.tool.autotest.spi.plugin.PluginRuntime;
import org.openbear.tool.autotest.spi.plugin.PluginRuntimeContext;
import org.openbear.tool.autotest.spi.step.StepTypeProvider;

/** Public-SPI descriptor for the HTTP plugin during the legacy handler migration. */
public final class HttpAutotestPlugin implements AutotestPlugin {
  @Override
  public PluginDescriptor descriptor() {
    return new PluginDescriptor("http", "HTTP Transport", "1.0.0", SpiVersion.CURRENT);
  }

  @Override
  public Collection<? extends StepTypeProvider<?, ?>> stepTypes() {
    return List.of(new HttpStepTypeProvider());
  }

  @Override
  public PluginRuntime open(PluginRuntimeContext context) {
    return new PluginRuntime() {
      @Override
      public Collection<? extends org.openbear.tool.autotest.spi.step.StepHandler<?>>
          stepHandlers() {
        return List.of(new PublicHttpStepHandler());
      }
    };
  }
}
