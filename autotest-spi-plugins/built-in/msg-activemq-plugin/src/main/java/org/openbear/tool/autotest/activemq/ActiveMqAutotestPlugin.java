package org.openbear.tool.autotest.activemq;

import java.util.Collection;
import java.util.List;
import org.openbear.tool.autotest.spi.SpiVersion;
import org.openbear.tool.autotest.spi.plugin.AutotestPlugin;
import org.openbear.tool.autotest.spi.plugin.PluginDescriptor;
import org.openbear.tool.autotest.spi.plugin.PluginRuntime;
import org.openbear.tool.autotest.spi.plugin.PluginRuntimeContext;
import org.openbear.tool.autotest.spi.step.StepTypeProvider;

/** Public-SPI descriptor for the ActiveMQ plugin during the handler migration. */
public final class ActiveMqAutotestPlugin implements AutotestPlugin {
  @Override
  public PluginDescriptor descriptor() {
    return new PluginDescriptor("activemq", "ActiveMQ JMS", "1.0.0", SpiVersion.CURRENT);
  }

  @Override
  public Collection<? extends StepTypeProvider<?, ?>> stepTypes() {
    return List.of(new AwaitMessageStepTypeProvider());
  }

  @Override
  public PluginRuntime open(PluginRuntimeContext context) {
    PublicAwaitMessageStepHandler handler = new PublicAwaitMessageStepHandler();
    return new PluginRuntime() {
      @Override
      public Collection<? extends org.openbear.tool.autotest.spi.step.StepHandler<?>>
          stepHandlers() {
        return List.of(handler);
      }

      @Override
      public void close() {
        handler.close();
      }
    };
  }
}
