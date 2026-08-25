package org.openbear.tool.autotest.core.engine;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.openbear.tool.autotest.core.AutotestVersion;
import org.openbear.tool.autotest.core.domain.ExecutionPlan;
import org.openbear.tool.autotest.core.domain.RunId;
import org.openbear.tool.autotest.core.event.ExecutionEventDispatcher;
import org.openbear.tool.autotest.core.event.RunFinished;
import org.openbear.tool.autotest.core.event.RunStarted;
import org.openbear.tool.autotest.core.model.ResultStatus;
import org.openbear.tool.autotest.core.model.RunResult;
import org.openbear.tool.autotest.core.model.ScenarioResult;
import org.openbear.tool.autotest.core.scheduler.ScenarioScheduler;
import org.openbear.tool.autotest.core.scheduler.SchedulingOptions;
import org.openbear.tool.autotest.core.scheduler.VirtualThreadScenarioScheduler;

/** Coordinates public-SPI scenarios with the normal scheduler and event stream. */
public final class PublicRunCoordinator {
  private final PublicScenarioRunner scenarios;
  private final Clock clock;
  private final ScenarioScheduler scheduler;

  public PublicRunCoordinator(PublicScenarioRunner scenarios, Clock clock) {
    this(scenarios, clock, new VirtualThreadScenarioScheduler());
  }

  PublicRunCoordinator(PublicScenarioRunner scenarios, Clock clock, ScenarioScheduler scheduler) {
    this.scenarios = Objects.requireNonNull(scenarios, "scenarios");
    this.clock = Objects.requireNonNull(clock, "clock");
    this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
  }

  public RunResult run(ExecutionPlan plan, RunRequest request) {
    Instant started = clock.instant();
    RunId runId = request.runId();
    ExecutionEventDispatcher events = new ExecutionEventDispatcher(clock, request.listeners());
    RunResult result = new RunResult();
    result.setToolVersion(AutotestVersion.VERSION);
    result.setRunId(runId.value());
    result.setLabel(request.label());
    result.setProject(plan.projectName());
    result.setEnvironment(plan.environment().name());
    result.setGitCommit(plan.metadata().gitCommit());
    result.setStartedAt(started);
    events.publish(new RunStarted(runId, plan.projectName(), plan.environment().id()));
    try {
      List<ScenarioResult> completed =
          scheduler.execute(
              runId,
              plan.scenarios(),
              scenario -> scenarios.run(runId.value(), scenario, plan.commonVariables(), events),
              new SchedulingOptions(plan.settings().parallelism(), plan.settings().failFast()));
      result.setScenarios(completed);
      result.setStatus(
          completed.stream().anyMatch(scenario -> scenario.getStatus().isFailure())
              ? ResultStatus.FAIL
              : ResultStatus.PASS);
      finish(result, started);
      events.publish(
          new RunFinished(
              runId,
              result.getStatus(),
              Duration.ofMillis(result.getDurationMs()),
              completed.size(),
              null));
      return result;
    } catch (RuntimeException | Error failure) {
      result.setStatus(ResultStatus.ERROR);
      finish(result, started);
      events.publish(
          new RunFinished(
              runId,
              ResultStatus.ERROR,
              Duration.ofMillis(result.getDurationMs()),
              result.getScenarios().size(),
              failureMessage(failure)));
      throw failure;
    }
  }

  private void finish(RunResult result, Instant started) {
    Instant ended = clock.instant();
    result.setEndedAt(ended);
    result.setDurationMs(Duration.between(started, ended).toMillis());
    result.setMetadata(summary(result.getScenarios()));
  }

  private static String failureMessage(Throwable failure) {
    String message = failure.getMessage();
    return message == null || message.isBlank()
        ? failure.getClass().getSimpleName()
        : failure.getClass().getSimpleName() + ": " + message;
  }

  private static Map<String, Object> summary(List<ScenarioResult> results) {
    Map<String, Object> summary = new LinkedHashMap<>();
    summary.put("total", results.size());
    summary.put(
        "passed",
        results.stream().filter(result -> result.getStatus() == ResultStatus.PASS).count());
    summary.put(
        "failed", results.stream().filter(result -> result.getStatus().isFailure()).count());
    return summary;
  }
}
