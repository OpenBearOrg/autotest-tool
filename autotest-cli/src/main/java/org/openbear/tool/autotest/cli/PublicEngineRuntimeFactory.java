package org.openbear.tool.autotest.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Clock;
import java.util.Objects;
import org.openbear.tool.autotest.core.domain.CompiledEnvironmentView;
import org.openbear.tool.autotest.core.domain.ExecutionPlan;
import org.openbear.tool.autotest.core.engine.EngineRuntime;
import org.openbear.tool.autotest.core.engine.EngineRuntimeFactory;
import org.openbear.tool.autotest.core.engine.PublicRunCoordinator;
import org.openbear.tool.autotest.core.engine.PublicScenarioRunner;
import org.openbear.tool.autotest.core.engine.RunRequest;
import org.openbear.tool.autotest.core.model.RunResult;
import org.openbear.tool.autotest.core.plugin.PluginRegistry;
import org.openbear.tool.autotest.core.secret.EnvironmentSecretProvider;
import org.openbear.tool.autotest.core.util.Workspace;
import org.openbear.tool.autotest.spi.plugin.PluginRuntimeContext;
import org.openbear.tool.autotest.spi.service.ResourceAccess;

/** Opens one public-SPI runtime for a compiled execution plan. */
final class PublicEngineRuntimeFactory implements EngineRuntimeFactory {
  private final Workspace workspace;
  private final PluginRegistry plugins;

  PublicEngineRuntimeFactory(Workspace workspace, PluginRegistry plugins) {
    this.workspace = Objects.requireNonNull(workspace, "workspace");
    this.plugins = Objects.requireNonNull(plugins, "plugins");
  }

  @Override
  public EngineRuntime open(ExecutionPlan plan, Clock clock) {
    plugins.open(
        new PluginRuntimeContext(
            new CompiledEnvironmentView(plan.environment()),
            new WorkspaceResources(workspace),
            new EnvironmentSecretProvider()::resolve,
            clock));
    if (plan.settings().runDoctorChecks()) {
      System.out.println("Preflight:");
      if (!CliSupport.printPublicDoctor(CliSupport.doctor(plugins))) {
        plugins.close();
        throw new EnvironmentFailureException("Environment doctor checks failed");
      }
      System.out.println();
    }
    PublicRunCoordinator coordinator =
        new PublicRunCoordinator(
            new PublicScenarioRunner(plugins, plan.environment(), workspace, clock), clock);
    return new EngineRuntime() {
      @Override
      public RunResult execute(ExecutionPlan ignored, RunRequest request) {
        return coordinator.run(plan, request);
      }

      @Override
      public void close() {
        plugins.close();
      }
    };
  }

  static record WorkspaceResources(Workspace workspace) implements ResourceAccess {
    @Override
    public String readText(String path) {
      try {
        return workspace.readText(path);
      } catch (IOException failure) {
        throw new IllegalArgumentException("Unable to read resource: " + path, failure);
      }
    }

    @Override
    public byte[] readBytes(String path) {
      try {
        return Files.readAllBytes(workspace.resolve(path));
      } catch (IOException failure) {
        throw new IllegalArgumentException("Unable to read resource: " + path, failure);
      }
    }
  }
}
