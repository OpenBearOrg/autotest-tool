package org.openbear.tool.autotest.core.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openbear.tool.autotest.core.domain.CompiledEnvironment;
import org.openbear.tool.autotest.core.domain.CompiledEnvironmentView;
import org.openbear.tool.autotest.core.domain.CompiledScenario;
import org.openbear.tool.autotest.core.domain.CompiledStep;
import org.openbear.tool.autotest.core.domain.EnvironmentId;
import org.openbear.tool.autotest.core.domain.PollingSettings;
import org.openbear.tool.autotest.core.domain.ScenarioExecutionPolicy;
import org.openbear.tool.autotest.core.domain.ScenarioId;
import org.openbear.tool.autotest.core.domain.ScenarioSource;
import org.openbear.tool.autotest.core.domain.StepId;
import org.openbear.tool.autotest.core.event.ExecutionEvent;
import org.openbear.tool.autotest.core.event.ExecutionEventDispatcher;
import org.openbear.tool.autotest.core.event.ScenarioFinished;
import org.openbear.tool.autotest.core.event.StepFinished;
import org.openbear.tool.autotest.core.model.ResultStatus;
import org.openbear.tool.autotest.core.model.ScenarioResult;
import org.openbear.tool.autotest.core.plugin.PluginRegistry;
import org.openbear.tool.autotest.core.util.Workspace;
import org.openbear.tool.autotest.spi.SpiVersion;
import org.openbear.tool.autotest.spi.plugin.AutotestPlugin;
import org.openbear.tool.autotest.spi.plugin.PluginDescriptor;
import org.openbear.tool.autotest.spi.plugin.PluginRuntime;
import org.openbear.tool.autotest.spi.plugin.PluginRuntimeContext;
import org.openbear.tool.autotest.spi.service.ResourceAccess;
import org.openbear.tool.autotest.spi.service.Secrets;
import org.openbear.tool.autotest.spi.step.ExecutableStep;
import org.openbear.tool.autotest.spi.step.StepExecutionContext;
import org.openbear.tool.autotest.spi.step.StepExecutionResult;
import org.openbear.tool.autotest.spi.step.StepHandler;
import org.openbear.tool.autotest.spi.step.StepIdentity;

class PublicScenarioRunnerTest {
  @TempDir Path workspaceRoot;

  @Test
  void classifiesDirectAssertionMismatchAsFailureAndCompletesEvents() {
    List<ExecutionEvent> events = new ArrayList<>();
    ScenarioResult result = run(List.of(step("assert", false, Behavior.ASSERT)), List.of(), events);

    assertEquals(ResultStatus.FAIL, result.getStatus());
    assertEquals(ResultStatus.FAIL, result.getSteps().getFirst().getStatus());
    assertTrue(
        result.getSteps().getFirst().getError().contains("expected expected but was actual"));
    assertEquals(1, events.stream().filter(StepFinished.class::isInstance).count());
    assertEquals(1, events.stream().filter(ScenarioFinished.class::isInstance).count());
    assertEquals(
        ResultStatus.FAIL,
        events.stream()
            .filter(StepFinished.class::isInstance)
            .map(StepFinished.class::cast)
            .findFirst()
            .orElseThrow()
            .status());
    assertEquals(
        ResultStatus.FAIL,
        events.stream()
            .filter(ScenarioFinished.class::isInstance)
            .map(ScenarioFinished.class::cast)
            .findFirst()
            .orElseThrow()
            .status());
  }

  @Test
  void classifiesVerifyValuesMismatchAsFailure() {
    ScenarioResult result =
        run(
            List.of(step("assert-values", false, Behavior.ASSERT_VALUES)),
            List.of(),
            new ArrayList<>());

    assertEquals(ResultStatus.FAIL, result.getStatus());
    assertEquals(ResultStatus.FAIL, result.getSteps().getFirst().getStatus());
    assertTrue(result.getSteps().getFirst().getError().contains("$.name"));
  }

  @Test
  void honorsContinueOnFailureForAssertionMismatches() {
    ScenarioResult stop =
        run(
            List.of(step("fails", false, Behavior.ASSERT), step("after", false, Behavior.PASS)),
            List.of(),
            new ArrayList<>());
    ScenarioResult continueAfterFailure =
        run(
            List.of(step("fails", true, Behavior.ASSERT), step("after", false, Behavior.PASS)),
            List.of(),
            new ArrayList<>());

    assertEquals(ResultStatus.SKIPPED, stop.getSteps().get(1).getStatus());
    assertEquals(ResultStatus.PASS, continueAfterFailure.getSteps().get(1).getStatus());
  }

  @Test
  void executesCleanupAfterAnAssertionMismatch() {
    AtomicBoolean cleanupRan = new AtomicBoolean();
    ScenarioResult result =
        run(
            List.of(step("fails", false, Behavior.ASSERT)),
            List.of(step("cleanup", false, Behavior.record(cleanupRan))),
            new ArrayList<>());

    assertTrue(cleanupRan.get());
    assertEquals(ResultStatus.PASS, result.getCleanup().getFirst().getStatus());
    assertEquals(ResultStatus.FAIL, result.getStatus());
  }

  @Test
  void skipsMainStepsAndRunsCleanupAfterSetupAssertionMismatch() {
    AtomicBoolean cleanupRan = new AtomicBoolean();
    ScenarioResult result =
        run(
            List.of(step("main", false, Behavior.PASS)),
            List.of(step("cleanup", false, Behavior.record(cleanupRan))),
            new ArrayList<>(),
            List.of(step("setup", false, Behavior.ASSERT)));

    assertEquals(ResultStatus.FAIL, result.getSetup().getFirst().getStatus());
    assertEquals(ResultStatus.SKIPPED, result.getSteps().getFirst().getStatus());
    assertTrue(cleanupRan.get());
    assertEquals(ResultStatus.FAIL, result.getStatus());
  }

  @Test
  void publishesErrorEventsBeforeRethrowingASeriousError() {
    List<ExecutionEvent> events = new ArrayList<>();

    assertThrows(
        AssertionError.class,
        () -> run(List.of(step("fatal", false, Behavior.SERIOUS_ERROR)), List.of(), events));

    assertEquals(
        ResultStatus.ERROR,
        events.stream()
            .filter(StepFinished.class::isInstance)
            .map(StepFinished.class::cast)
            .findFirst()
            .orElseThrow()
            .status());
    assertEquals(
        ResultStatus.ERROR,
        events.stream()
            .filter(ScenarioFinished.class::isInstance)
            .map(ScenarioFinished.class::cast)
            .findFirst()
            .orElseThrow()
            .status());
  }

  private ScenarioResult run(
      List<CompiledStep> steps, List<CompiledStep> cleanup, List<ExecutionEvent> events) {
    return run(steps, cleanup, events, List.of());
  }

  private ScenarioResult run(
      List<CompiledStep> steps,
      List<CompiledStep> cleanup,
      List<ExecutionEvent> events,
      List<CompiledStep> setup) {
    CompiledEnvironment environment = environment();
    try (PluginRegistry plugins = registry()) {
      PublicScenarioRunner runner =
          new PublicScenarioRunner(
              plugins, environment, new Workspace(workspaceRoot), Clock.systemUTC());
      return runner.run(
          "run",
          new CompiledScenario(
              new ScenarioId("scenario"),
              "Scenario",
              Set.of(),
              Set.of(),
              Map.of(),
              setup,
              steps,
              cleanup,
              ScenarioExecutionPolicy.SEQUENTIAL,
              new ScenarioSource(workspaceRoot.resolve("scenario.yaml"), "checksum", Map.of())),
          Map.of(),
          new ExecutionEventDispatcher(
              Clock.systemUTC(), List.of(envelope -> events.add(envelope.event()))));
    }
  }

  private PluginRegistry registry() {
    PluginRegistry registry = new PluginRegistry().register(new TestPlugin());
    registry.open(
        new PluginRuntimeContext(
            new CompiledEnvironmentView(environment()), resources(), secrets(), Clock.systemUTC()));
    return registry;
  }

  private static CompiledStep step(String id, boolean continueOnFailure, Behavior behavior) {
    return new CompiledStep(
        new StepId(id),
        null,
        null,
        continueOnFailure,
        "test",
        Map.of(),
        new TestStep(new StepIdentity(id, null, null, continueOnFailure), behavior));
  }

  private static CompiledEnvironment environment() {
    return new CompiledEnvironment(
        new EnvironmentId("test"),
        Map.of(),
        Map.of(),
        Map.of(),
        false,
        new PollingSettings(Duration.ofSeconds(1), Duration.ofMillis(1)));
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

  private record TestStep(StepIdentity identity, Behavior behavior) implements ExecutableStep {
    @Override
    public String type() {
      return "test";
    }
  }

  private static final class TestPlugin implements AutotestPlugin {
    @Override
    public PluginDescriptor descriptor() {
      return new PluginDescriptor("test", "Test", "1", SpiVersion.CURRENT);
    }

    @Override
    public PluginRuntime open(PluginRuntimeContext context) {
      return new PluginRuntime() {
        @Override
        public List<StepHandler<?>> stepHandlers() {
          return List.of(new TestStepHandler());
        }
      };
    }
  }

  private static final class TestStepHandler implements StepHandler<TestStep> {
    @Override
    public Class<TestStep> stepType() {
      return TestStep.class;
    }

    @Override
    public StepExecutionResult execute(TestStep step, StepExecutionContext context) {
      if (step.behavior() == Behavior.ASSERT)
        context.services().assertions().verify("actual", "expected");
      if (step.behavior() == Behavior.ASSERT_VALUES)
        context
            .services()
            .assertions()
            .verifyValues(Map.of("name", "actual"), Map.of("$.name", "expected"));
      if (step.behavior() == Behavior.SERIOUS_ERROR) throw new AssertionError("serious");
      step.behavior().marker().ifPresent(recorded -> recorded.set(true));
      return StepExecutionResult.success(Map.of(), Map.of());
    }
  }

  private record Behavior(Kind kind, AtomicBoolean recorded) {
    private static final Behavior ASSERT = new Behavior(Kind.ASSERT, null);
    private static final Behavior ASSERT_VALUES = new Behavior(Kind.ASSERT_VALUES, null);
    private static final Behavior PASS = new Behavior(Kind.PASS, null);
    private static final Behavior SERIOUS_ERROR = new Behavior(Kind.SERIOUS_ERROR, null);

    private static Behavior record(AtomicBoolean recorded) {
      return new Behavior(Kind.PASS, recorded);
    }

    private java.util.Optional<AtomicBoolean> marker() {
      return java.util.Optional.ofNullable(recorded);
    }
  }

  private enum Kind {
    ASSERT,
    ASSERT_VALUES,
    PASS,
    SERIOUS_ERROR
  }
}
