package org.openbear.tool.autotest.fixture;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.openbear.tool.autotest.spi.SpiVersion;
import org.openbear.tool.autotest.spi.plugin.AutotestPlugin;
import org.openbear.tool.autotest.spi.plugin.PluginDescriptor;
import org.openbear.tool.autotest.spi.plugin.PluginRuntime;
import org.openbear.tool.autotest.spi.plugin.PluginRuntimeContext;
import org.openbear.tool.autotest.spi.resource.PluginResource;
import org.openbear.tool.autotest.spi.resource.ResourceCompileContext;
import org.openbear.tool.autotest.spi.resource.ResourceTypeProvider;
import org.openbear.tool.autotest.spi.step.ExecutableStep;
import org.openbear.tool.autotest.spi.step.StepCompileContext;
import org.openbear.tool.autotest.spi.step.StepConfiguration;
import org.openbear.tool.autotest.spi.step.StepExecutionContext;
import org.openbear.tool.autotest.spi.step.StepExecutionResult;
import org.openbear.tool.autotest.spi.step.StepHandler;
import org.openbear.tool.autotest.spi.step.StepIdentity;
import org.openbear.tool.autotest.spi.step.StepTypeProvider;

/** Test-only external plugin deliberately compiled against the public SPI alone. */
public final class FixturePlugin implements AutotestPlugin {
  @Override
  public PluginDescriptor descriptor() {
    return new PluginDescriptor("fixture", "External SPI fixture", "1.0.0", SpiVersion.CURRENT);
  }

  @Override
  public Collection<? extends StepTypeProvider<?, ?>> stepTypes() {
    return List.of(new EchoType());
  }

  @Override
  public Collection<? extends ResourceTypeProvider<?, ?>> resourceTypes() {
    return List.of(new FixtureResourceType());
  }

  @Override
  public PluginRuntime open(PluginRuntimeContext context) {
    return new PluginRuntime() {
      @Override
      public Collection<? extends StepHandler<?>> stepHandlers() {
        return List.of(new EchoHandler());
      }
    };
  }

  public record EchoConfiguration(String id, String message) implements StepConfiguration {
    @Override
    public StepIdentity identity() {
      return new StepIdentity(id, null, null, false);
    }
  }

  public record EchoStep(StepIdentity identity, String message) implements ExecutableStep {
    @Override
    public String type() {
      return "test.echo";
    }
  }

  public record FixtureResource(String name, String prefix) implements PluginResource {
    @Override
    public String type() {
      return "test.resource";
    }
  }

  public static final class EchoType implements StepTypeProvider<EchoConfiguration, EchoStep> {
    @Override
    public String type() {
      return "test.echo";
    }

    @Override
    public Class<EchoConfiguration> configurationType() {
      return EchoConfiguration.class;
    }

    @Override
    public String schemaResource() {
      return "";
    }

    @Override
    public EchoStep compile(EchoConfiguration config, StepCompileContext context) {
      return new EchoStep(config.identity(), config.message());
    }

    @Override
    public Class<EchoStep> executableType() {
      return EchoStep.class;
    }
  }

  public static final class FixtureResourceType
      implements ResourceTypeProvider<Map<String, Object>, FixtureResource> {
    @Override
    public String type() {
      return "test.resource";
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<Map<String, Object>> configurationType() {
      return (Class<Map<String, Object>>) (Class<?>) Map.class;
    }

    @Override
    public String schemaResource() {
      return "";
    }

    @Override
    public FixtureResource compile(
        String name, Map<String, Object> config, ResourceCompileContext context) {
      return new FixtureResource(name, String.valueOf(config.get("prefix")));
    }

    @Override
    public Class<FixtureResource> resourceType() {
      return FixtureResource.class;
    }
  }

  public static final class EchoHandler implements StepHandler<EchoStep> {
    @Override
    public Class<EchoStep> stepType() {
      return EchoStep.class;
    }

    @Override
    public StepExecutionResult execute(EchoStep step, StepExecutionContext context) {
      context.services().variables().put("fixtureMessage", step.message());
      return StepExecutionResult.success(
          Map.of("fixtureMessage", step.message()), Map.of("echo", step.message()));
    }
  }
}
