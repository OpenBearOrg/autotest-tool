package org.openbear.tool.autotest.core.plugin.builtin;

import java.util.Collection;
import java.util.List;
import org.openbear.tool.autotest.core.AutotestVersion;
import org.openbear.tool.autotest.spi.SpiVersion;
import org.openbear.tool.autotest.spi.plugin.AutotestPlugin;
import org.openbear.tool.autotest.spi.plugin.PluginDescriptor;
import org.openbear.tool.autotest.spi.plugin.PluginRuntime;
import org.openbear.tool.autotest.spi.plugin.PluginRuntimeContext;
import org.openbear.tool.autotest.spi.step.StepCompileContext;
import org.openbear.tool.autotest.spi.step.StepHandler;
import org.openbear.tool.autotest.spi.step.StepTypeProvider;

/** Core-owned DSL providers retained while transport steps migrate to plugins. */
public final class BuiltInStepPlugin implements AutotestPlugin {
  private final StepTypeProvider<SetConfiguration, SetExecutableStep> set =
      new StepTypeProvider<>() {
        @Override
        public String type() {
          return "set";
        }

        @Override
        public Class<SetConfiguration> configurationType() {
          return SetConfiguration.class;
        }

        @Override
        public String schemaResource() {
          return "";
        }

        @Override
        public SetExecutableStep compile(
            SetConfiguration configuration, StepCompileContext context) {
          return new SetExecutableStep(configuration.identity(), configuration.values());
        }

        @Override
        public Class<SetExecutableStep> executableType() {
          return SetExecutableStep.class;
        }
      };
  private final StepTypeProvider<AssertConfiguration, AssertExecutableStep> assertStep =
      new StepTypeProvider<>() {
        @Override
        public String type() {
          return "assert";
        }

        @Override
        public Class<AssertConfiguration> configurationType() {
          return AssertConfiguration.class;
        }

        @Override
        public String schemaResource() {
          return "";
        }

        @Override
        public AssertExecutableStep compile(
            AssertConfiguration configuration, StepCompileContext context) {
          return new AssertExecutableStep(configuration.identity(), configuration.values());
        }

        @Override
        public Class<AssertExecutableStep> executableType() {
          return AssertExecutableStep.class;
        }
      };

  @Override
  public PluginDescriptor descriptor() {
    return new PluginDescriptor(
        "autotest-core", "Autotest built-in steps", AutotestVersion.VERSION, SpiVersion.CURRENT);
  }

  @Override
  public Collection<? extends StepTypeProvider<?, ?>> stepTypes() {
    return List.of(set, assertStep);
  }

  @Override
  public PluginRuntime open(PluginRuntimeContext context) {
    return new PluginRuntime() {
      @Override
      public Collection<? extends StepHandler<?>> stepHandlers() {
        return List.of(new PublicSetStepHandler(), new PublicAssertStepHandler());
      }
    };
  }
}
