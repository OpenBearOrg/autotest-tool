package org.openbear.tool.autotest.core.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openbear.tool.autotest.core.domain.CompilationMetadata;
import org.openbear.tool.autotest.core.domain.CompiledEnvironment;
import org.openbear.tool.autotest.core.domain.CompiledScenario;
import org.openbear.tool.autotest.core.domain.EnvironmentId;
import org.openbear.tool.autotest.core.domain.ExecutionPlan;
import org.openbear.tool.autotest.core.domain.ExecutionSettings;
import org.openbear.tool.autotest.core.domain.PollingSettings;
import org.openbear.tool.autotest.core.domain.RunId;
import org.openbear.tool.autotest.core.domain.ScenarioExecutionPolicy;
import org.openbear.tool.autotest.core.domain.ScenarioId;
import org.openbear.tool.autotest.core.domain.ScenarioSource;
import org.openbear.tool.autotest.core.event.ExecutionEvent;
import org.openbear.tool.autotest.core.event.ExecutionListener;
import org.openbear.tool.autotest.core.event.RunFinished;
import org.openbear.tool.autotest.core.event.RunStarted;
import org.openbear.tool.autotest.core.model.ResultStatus;
import org.openbear.tool.autotest.core.model.RunResult;
import org.openbear.tool.autotest.core.model.ScenarioResult;
import org.openbear.tool.autotest.core.plugin.PluginRegistry;
import org.openbear.tool.autotest.core.scheduler.ScenarioScheduler;
import org.openbear.tool.autotest.core.util.Workspace;

class PublicRunCoordinatorTest {
  @TempDir Path workspaceRoot;

  @Test
  void emitsOnePassingTerminalEventForASuccessfulRun() {
    List<ExecutionEvent> events = new ArrayList<>();
    RunResult result = run(schedulerReturning(ResultStatus.PASS), events);

    assertEquals(ResultStatus.PASS, result.getStatus());
    assertEquals(1, events.stream().filter(RunStarted.class::isInstance).count());
    assertEquals(1, events.stream().filter(RunFinished.class::isInstance).count());
    RunFinished finished = terminal(events);
    assertEquals(ResultStatus.PASS, finished.status());
    assertEquals(1, finished.scenarioCount());
    assertEquals(null, finished.message());
  }

  @Test
  void emitsOneFailingTerminalEventForANormalScenarioFailure() {
    List<ExecutionEvent> events = new ArrayList<>();
    RunResult result = run(schedulerReturning(ResultStatus.FAIL), events);

    assertEquals(ResultStatus.FAIL, result.getStatus());
    assertEquals(1, events.stream().filter(RunFinished.class::isInstance).count());
    assertEquals(ResultStatus.FAIL, terminal(events).status());
  }

  @Test
  void emitsAnErrorTerminalEventBeforeRethrowingASchedulerRuntimeFailure() {
    List<ExecutionEvent> events = new ArrayList<>();
    IllegalStateException failure = new IllegalStateException("boom");

    IllegalStateException thrown =
        assertThrows(IllegalStateException.class, () -> run(throwing(failure), events));

    assertSame(failure, thrown);
    assertEquals(1, events.stream().filter(RunFinished.class::isInstance).count());
    RunFinished finished = terminal(events);
    assertEquals(ResultStatus.ERROR, finished.status());
    assertEquals(0, finished.scenarioCount());
    assertEquals("IllegalStateException: boom", finished.message());
  }

  @Test
  void emitsAnErrorTerminalEventBeforeRethrowingASeriousError() {
    List<ExecutionEvent> events = new ArrayList<>();
    AssertionError failure = new AssertionError("serious");

    AssertionError thrown =
        assertThrows(AssertionError.class, () -> run(throwing(failure), events));

    assertSame(failure, thrown);
    assertEquals(ResultStatus.ERROR, terminal(events).status());
    assertEquals("AssertionError: serious", terminal(events).message());
  }

  @Test
  void aBrokenListenerDoesNotPreventOtherListenersFromReceivingTheTerminalEvent() {
    List<ExecutionEvent> events = new ArrayList<>();
    ExecutionListener broken =
        envelope -> {
          throw new IllegalStateException("listener failed");
        };
    ExecutionListener collecting = envelope -> events.add(envelope.event());
    PublicRunCoordinator coordinator = coordinator(schedulerReturning(ResultStatus.PASS));

    RunResult result =
        coordinator.run(
            plan(), new RunRequest(new RunId("run"), null, List.of(broken, collecting)));

    assertEquals(ResultStatus.PASS, result.getStatus());
    assertEquals(1, events.stream().filter(RunFinished.class::isInstance).count());
  }

  private RunResult run(ScenarioScheduler scheduler, List<ExecutionEvent> events) {
    return coordinator(scheduler)
        .run(
            plan(),
            new RunRequest(
                new RunId("run"), null, List.of(envelope -> events.add(envelope.event()))));
  }

  private PublicRunCoordinator coordinator(ScenarioScheduler scheduler) {
    Clock clock = Clock.systemUTC();
    return new PublicRunCoordinator(
        new PublicScenarioRunner(
            new PluginRegistry(), environment(), new Workspace(workspaceRoot), clock),
        clock,
        scheduler);
  }

  private ScenarioScheduler schedulerReturning(ResultStatus status) {
    return new ScenarioScheduler() {
      @Override
      public <T> List<ScenarioResult> execute(
          RunId runId,
          List<T> scenarios,
          org.openbear.tool.autotest.core.scheduler.ScenarioExecutionFunction<T> execution,
          org.openbear.tool.autotest.core.scheduler.SchedulingOptions options) {
        return List.of(scenarioResult(status));
      }
    };
  }

  private static ScenarioScheduler throwing(Error failure) {
    return new ScenarioScheduler() {
      @Override
      public <T> List<ScenarioResult> execute(
          RunId runId,
          List<T> scenarios,
          org.openbear.tool.autotest.core.scheduler.ScenarioExecutionFunction<T> execution,
          org.openbear.tool.autotest.core.scheduler.SchedulingOptions options) {
        throw failure;
      }
    };
  }

  private static ScenarioScheduler throwing(RuntimeException failure) {
    return new ScenarioScheduler() {
      @Override
      public <T> List<ScenarioResult> execute(
          RunId runId,
          List<T> scenarios,
          org.openbear.tool.autotest.core.scheduler.ScenarioExecutionFunction<T> execution,
          org.openbear.tool.autotest.core.scheduler.SchedulingOptions options) {
        throw failure;
      }
    };
  }

  private static ScenarioResult scenarioResult(ResultStatus status) {
    ScenarioResult result = new ScenarioResult();
    result.setStatus(status);
    return result;
  }

  private ExecutionPlan plan() {
    return new ExecutionPlan(
        "project",
        environment(),
        List.of(
            new CompiledScenario(
                new ScenarioId("scenario"),
                "Scenario",
                Set.of(),
                Set.of(),
                Map.of(),
                List.of(),
                List.of(),
                List.of(),
                ScenarioExecutionPolicy.SEQUENTIAL,
                new ScenarioSource(workspaceRoot.resolve("scenario.yaml"), "checksum", Map.of()))),
        Map.of(),
        new ExecutionSettings(1, false, false),
        Set.of(),
        new CompilationMetadata("1.0", null, Instant.EPOCH));
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

  private static RunFinished terminal(List<ExecutionEvent> events) {
    return events.stream()
        .filter(RunFinished.class::isInstance)
        .map(RunFinished.class::cast)
        .findFirst()
        .orElseThrow();
  }
}
