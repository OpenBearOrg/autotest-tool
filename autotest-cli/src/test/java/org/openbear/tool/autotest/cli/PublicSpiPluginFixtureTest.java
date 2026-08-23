package org.openbear.tool.autotest.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openbear.tool.autotest.core.domain.CompilationMetadata;
import org.openbear.tool.autotest.core.domain.CompiledEnvironment;
import org.openbear.tool.autotest.core.domain.CompiledEnvironmentView;
import org.openbear.tool.autotest.core.domain.CompiledScenario;
import org.openbear.tool.autotest.core.domain.CompiledStep;
import org.openbear.tool.autotest.core.domain.EnvironmentId;
import org.openbear.tool.autotest.core.domain.ExecutionPlan;
import org.openbear.tool.autotest.core.domain.ExecutionSettings;
import org.openbear.tool.autotest.core.domain.PollingSettings;
import org.openbear.tool.autotest.core.domain.ScenarioExecutionPolicy;
import org.openbear.tool.autotest.core.domain.ScenarioId;
import org.openbear.tool.autotest.core.domain.ScenarioSource;
import org.openbear.tool.autotest.core.domain.StepId;
import org.openbear.tool.autotest.core.engine.PublicRunCoordinator;
import org.openbear.tool.autotest.core.engine.PublicScenarioRunner;
import org.openbear.tool.autotest.core.engine.RunRequest;
import org.openbear.tool.autotest.core.plugin.PluginRegistry;
import org.openbear.tool.autotest.core.util.Workspace;
import org.openbear.tool.autotest.fixture.FixturePlugin;
import org.openbear.tool.autotest.spi.plugin.PluginRuntimeContext;
import org.openbear.tool.autotest.spi.service.ResourceAccess;
import org.openbear.tool.autotest.spi.service.Secrets;

class PublicSpiPluginFixtureTest {
  @TempDir Path temp;

  @Test
  void discoversAndExecutesFixtureThroughThePublicSpi() throws Exception {
    Path fixtureRoot =
        Path.of(FixturePlugin.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    try (PluginRegistry registry =
        PluginBootstrap.dynamicRegistry(
            new ServiceResourceFilteringClassLoader(PluginBootstrap.class.getClassLoader()),
            java.util.List.of(fixtureRoot))) {
      assertTrue(registry.plugin("fixture").isPresent());
      assertTrue(registry.stepTypes().find("test.echo").isPresent());
      assertTrue(registry.resourceTypes().find("test.resource").isPresent());

      CompiledEnvironment environment =
          new CompiledEnvironment(
              new EnvironmentId("test"),
              Map.of(),
              Map.of(),
              Map.of(),
              false,
              new PollingSettings(Duration.ofSeconds(1), Duration.ofMillis(1)));
      registry.open(
          new PluginRuntimeContext(
              new CompiledEnvironmentView(environment), resources(), secrets(), Clock.systemUTC()));
      FixturePlugin.EchoStep step =
          new FixturePlugin.EchoStep(
              new org.openbear.tool.autotest.spi.step.StepIdentity("echo", null, null, false),
              "hello");
      CompiledScenario scenario =
          new CompiledScenario(
              new ScenarioId("fixture-scenario"),
              "Fixture scenario",
              java.util.Set.of(),
              java.util.Set.of(),
              Map.of(),
              List.of(),
              List.of(
                  new CompiledStep(
                      new StepId("echo"), null, null, false, "test.echo", Map.of(), step)),
              List.of(),
              ScenarioExecutionPolicy.SEQUENTIAL,
              new ScenarioSource(fixtureRoot.resolve("fixture.yaml"), "checksum", Map.of()));
      ExecutionPlan plan =
          new ExecutionPlan(
              "fixture-project",
              environment,
              List.of(scenario),
              Map.of(),
              new ExecutionSettings(1, false, false),
              java.util.Set.of(),
              new CompilationMetadata("1.0", null, Instant.EPOCH));
      var result =
          new PublicRunCoordinator(
                  new PublicScenarioRunner(
                      registry, environment, new Workspace(fixtureRoot), Clock.systemUTC()),
                  Clock.systemUTC())
              .run(
                  plan,
                  RunRequest.create(new org.openbear.tool.autotest.core.domain.RunId("run"), null));
      assertEquals(
          "hello",
          result.getScenarios().getFirst().getSteps().getFirst().getEvidence().get("echo"));
      assertEquals(
          "hello", result.getScenarios().getFirst().getFinalVariables().get("fixtureMessage"));
    }
  }

  @Test
  void runsAnExternalPluginThroughTheCli() throws Exception {
    Path fixtureRoot =
        Path.of(FixturePlugin.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    Files.createDirectories(temp.resolve("environments"));
    Files.createDirectories(temp.resolve("scenarios"));
    Files.writeString(
        temp.resolve("autotest-tool.yaml"),
        """
        projectVersion: "1.0"
        name: Fixture project
        defaults:
          environment: fixture
        execution:
          parallelism: 1
          failFast: false
        """);
    Files.writeString(
        temp.resolve("environments/fixture.yaml"),
        """
        environmentVersion: "1.0"
        name: fixture
        resources:
          test.resource:
            configured:
              prefix: hello
        """);
    Files.writeString(
        temp.resolve("scenarios/fixture.yaml"),
        """
        dslVersion: "1.0"
        id: fixture-scenario
        name: Fixture scenario
        steps:
          - test.echo:
              id: echo
              message: hello
        """);

    ClassLoader previous = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(new ServiceResourceFilteringClassLoader(previous));
    try {
      RunCommand command = new RunCommand();
      command.workspacePath = temp;
      command.pluginDirs = List.of(fixtureRoot);
      command.scenarioRefs = List.of("fixture-scenario");
      assertEquals(0, command.call());
    } finally {
      Thread.currentThread().setContextClassLoader(previous);
    }
    try (var files = Files.walk(temp.resolve("reports"))) {
      assertTrue(files.anyMatch(path -> path.getFileName().toString().equals("result.json")));
    }
  }

  private static final class ServiceResourceFilteringClassLoader extends ClassLoader {
    private ServiceResourceFilteringClassLoader(ClassLoader parent) {
      super(parent);
    }

    @Override
    public java.util.Enumeration<URL> getResources(String name) throws IOException {
      if (name.equals(
          "META-INF/services/"
              + org.openbear.tool.autotest.spi.plugin.AutotestPlugin.class.getName()))
        return java.util.Collections.emptyEnumeration();
      return super.getResources(name);
    }
  }

  private static ResourceAccess resources() {
    return new ResourceAccess() {
      @Override
      public String readText(String path) {
        return "";
      }

      @Override
      public byte[] readBytes(String path) {
        return new byte[0];
      }
    };
  }

  private static Secrets secrets() {
    return reference -> java.util.Optional.empty();
  }
}
