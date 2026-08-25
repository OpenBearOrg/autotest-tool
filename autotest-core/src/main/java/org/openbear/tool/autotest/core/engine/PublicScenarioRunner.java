package org.openbear.tool.autotest.core.engine;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.openbear.tool.autotest.core.domain.CompiledEnvironment;
import org.openbear.tool.autotest.core.domain.CompiledScenario;
import org.openbear.tool.autotest.core.domain.CompiledStep;
import org.openbear.tool.autotest.core.domain.ExecutionId;
import org.openbear.tool.autotest.core.domain.RunId;
import org.openbear.tool.autotest.core.domain.ScenarioId;
import org.openbear.tool.autotest.core.event.ExecutionEventDispatcher;
import org.openbear.tool.autotest.core.event.ScenarioFinished;
import org.openbear.tool.autotest.core.event.ScenarioStarted;
import org.openbear.tool.autotest.core.event.StepPhase;
import org.openbear.tool.autotest.core.model.ResultStatus;
import org.openbear.tool.autotest.core.model.ScenarioResult;
import org.openbear.tool.autotest.core.model.StepResult;
import org.openbear.tool.autotest.core.plugin.PluginRegistry;
import org.openbear.tool.autotest.core.util.Workspace;
import org.openbear.tool.autotest.spi.step.StepRuntimeServices;

/** Executes public-SPI executable steps without translating them to legacy DTOs. */
public final class PublicScenarioRunner {
  private final CompiledEnvironment environment;
  private final Clock clock;
  private final IdGenerator ids;
  private final PublicStepExecutor steps;
  private final StepRuntimeServicesFactory services;

  public PublicScenarioRunner(
      PluginRegistry plugins, CompiledEnvironment environment, Workspace workspace, Clock clock) {
    this(plugins, environment, workspace, clock, new DefaultIdGenerator(clock));
  }

  PublicScenarioRunner(
      PluginRegistry plugins,
      CompiledEnvironment environment,
      Workspace workspace,
      Clock clock,
      IdGenerator ids) {
    this.environment = Objects.requireNonNull(environment, "environment");
    this.clock = Objects.requireNonNull(clock, "clock");
    this.ids = Objects.requireNonNull(ids, "ids");
    steps = new PublicStepExecutor(plugins, clock);
    services = new StepRuntimeServicesFactory(environment, workspace, clock);
  }

  public ScenarioResult run(
      String runId,
      CompiledScenario scenario,
      Map<String, Object> commonVariables,
      ExecutionEventDispatcher events) {
    Instant started = clock.instant();
    ScenarioId scenarioId = scenario.id();
    ExecutionId executionId = ids.nextExecutionId(scenarioId);
    ScenarioResult result = initializeResult(scenario, executionId, started);
    events.publish(new ScenarioStarted(new RunId(runId), executionId, scenarioId, scenario.name()));

    ScenarioVariables variables =
        new ScenarioVariables(initialVariables(runId, executionId, scenario, commonVariables));
    StepRuntimeServices runtimeServices;
    try {
      variables.materializeRuntimeExpressions();
      runtimeServices = services.create(variables);
      try {
        List<String> missing =
            scenario.requiredVariables().stream()
                .filter(name -> !variables.find(name).isPresent())
                .toList();
        if (!missing.isEmpty())
          throw new IllegalArgumentException("Missing required variables: " + missing);

        boolean setupOk =
            executePhase(
                runId,
                executionId,
                scenarioId,
                scenario.setup(),
                result.getSetup(),
                runtimeServices,
                events,
                true);
        if (setupOk)
          executePhase(
              runId,
              executionId,
              scenarioId,
              scenario.steps(),
              result.getSteps(),
              runtimeServices,
              events,
              false);
        else
          for (CompiledStep step : scenario.steps())
            result
                .getSteps()
                .add(
                    steps.skipped(
                        runId,
                        executionId,
                        scenarioId,
                        step,
                        StepPhase.MAIN,
                        events,
                        "setup failed"));
      } catch (Exception failure) {
        result.setError(PublicStepExecutor.failureMessage(failure));
      } finally {
        executeCleanup(
            runId,
            executionId,
            scenarioId,
            scenario.cleanup(),
            result.getCleanup(),
            runtimeServices,
            events);
        result.setFinalVariables(variables.snapshot());
      }
    } catch (Error failure) {
      finish(result, started, ResultStatus.ERROR, PublicStepExecutor.failureMessage(failure));
      publishFinished(runId, executionId, scenarioId, result, events);
      throw failure;
    }

    ResultStatus status =
        result.getError() != null
                || failed(result.getSetup())
                || failed(result.getSteps())
                || failed(result.getCleanup())
            ? ResultStatus.FAIL
            : ResultStatus.PASS;
    finish(result, started, status, result.getError());
    publishFinished(runId, executionId, scenarioId, result, events);
    return result;
  }

  private ScenarioResult initializeResult(
      CompiledScenario scenario, ExecutionId executionId, Instant started) {
    ScenarioResult result = new ScenarioResult();
    result.setId(scenario.id().value());
    result.setName(scenario.name());
    result.setExecutionId(executionId.value());
    result.setChecksum(scenario.source().scenarioChecksum());
    result.setResourceChecksums(scenario.source().resourceChecksums());
    result.setStartedAt(started);
    return result;
  }

  private boolean executePhase(
      String runId,
      ExecutionId executionId,
      ScenarioId scenarioId,
      List<CompiledStep> phaseSteps,
      List<StepResult> output,
      StepRuntimeServices runtimeServices,
      ExecutionEventDispatcher events,
      boolean setup) {
    boolean healthy = true;
    for (CompiledStep step : phaseSteps) {
      StepPhase phase = setup ? StepPhase.SETUP : StepPhase.MAIN;
      StepResult result =
          healthy
              ? steps.execute(runId, executionId, scenarioId, step, phase, runtimeServices, events)
              : steps.skipped(
                  runId,
                  executionId,
                  scenarioId,
                  step,
                  phase,
                  events,
                  setup ? "previous setup step failed" : "previous step failed");
      output.add(result);
      if (result.getStatus().isFailure() && !step.continueOnFailure()) healthy = false;
    }
    return healthy;
  }

  private void executeCleanup(
      String runId,
      ExecutionId executionId,
      ScenarioId scenarioId,
      List<CompiledStep> cleanup,
      List<StepResult> output,
      StepRuntimeServices runtimeServices,
      ExecutionEventDispatcher events) {
    for (CompiledStep step : cleanup)
      output.add(
          steps.execute(
              runId, executionId, scenarioId, step, StepPhase.CLEANUP, runtimeServices, events));
  }

  private void finish(ScenarioResult result, Instant started, ResultStatus status, String error) {
    Instant ended = clock.instant();
    result.setEndedAt(ended);
    result.setDurationMs(Duration.between(started, ended).toMillis());
    result.setStatus(status);
    result.setError(error);
  }

  private void publishFinished(
      String runId,
      ExecutionId executionId,
      ScenarioId scenarioId,
      ScenarioResult result,
      ExecutionEventDispatcher events) {
    events.publish(
        new ScenarioFinished(
            new RunId(runId),
            executionId,
            scenarioId,
            result.getStatus(),
            Duration.ofMillis(result.getDurationMs()),
            result.getError()));
  }

  private Map<String, Object> initialVariables(
      String runId,
      ExecutionId executionId,
      CompiledScenario scenario,
      Map<String, Object> commonVariables) {
    Map<String, Object> values = new LinkedHashMap<>(scenario.variables());
    if (commonVariables != null) values.putAll(commonVariables);
    values.put("runId", runId);
    values.put("executionId", executionId.value());
    values.put("scenarioId", scenario.id().value());
    values.put("environment", environment.name());
    return values;
  }

  private static boolean failed(List<StepResult> results) {
    return results.stream().anyMatch(result -> result.getStatus().isFailure());
  }
}
