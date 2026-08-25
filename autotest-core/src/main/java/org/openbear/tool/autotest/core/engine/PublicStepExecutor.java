package org.openbear.tool.autotest.core.engine;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.openbear.tool.autotest.core.assertion.AssertionFailureException;
import org.openbear.tool.autotest.core.domain.CompiledStep;
import org.openbear.tool.autotest.core.domain.ExecutionId;
import org.openbear.tool.autotest.core.domain.RunId;
import org.openbear.tool.autotest.core.domain.ScenarioId;
import org.openbear.tool.autotest.core.event.ExecutionEventDispatcher;
import org.openbear.tool.autotest.core.event.StepFinished;
import org.openbear.tool.autotest.core.event.StepPhase;
import org.openbear.tool.autotest.core.event.StepStarted;
import org.openbear.tool.autotest.core.model.ResultStatus;
import org.openbear.tool.autotest.core.model.StepResult;
import org.openbear.tool.autotest.core.plugin.PluginRegistry;
import org.openbear.tool.autotest.spi.step.ExecutableStep;
import org.openbear.tool.autotest.spi.step.StepExecutionContext;
import org.openbear.tool.autotest.spi.step.StepExecutionResult;
import org.openbear.tool.autotest.spi.step.StepExecutionStatus;
import org.openbear.tool.autotest.spi.step.StepHandler;
import org.openbear.tool.autotest.spi.step.StepRuntimeServices;

/** Executes one public-SPI step and maps its outcome to the core result model. */
final class PublicStepExecutor {
  private final PluginRegistry plugins;
  private final Clock clock;

  PublicStepExecutor(PluginRegistry plugins, Clock clock) {
    this.plugins = Objects.requireNonNull(plugins, "plugins");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  StepResult execute(
      String runId,
      ExecutionId executionId,
      ScenarioId scenarioId,
      CompiledStep step,
      StepPhase phase,
      StepRuntimeServices services,
      ExecutionEventDispatcher events) {
    events.publish(
        new StepStarted(new RunId(runId), executionId, scenarioId, step.id(), step.type(), phase));
    Instant started = clock.instant();
    StepResult result;
    try {
      ExecutableStep executable =
          Objects.requireNonNull(
              step.executable(), "No public executable compiled for step " + step.id());
      StepExecutionResult execution =
          executeHandler(executable, runId, executionId, scenarioId, services);
      execution.captures().forEach(services.variables()::put);
      result = result(step, started, execution);
    } catch (AssertionFailureException failure) {
      result = result(step, started, ResultStatus.FAIL, failure.getMessage(), Map.of());
    } catch (Exception failure) {
      result = result(step, started, ResultStatus.ERROR, failureMessage(failure), Map.of());
    } catch (Error failure) {
      publishFinished(
          runId,
          executionId,
          scenarioId,
          step,
          phase,
          started,
          ResultStatus.ERROR,
          failureMessage(failure),
          events);
      throw failure;
    }
    publishFinished(
        runId,
        executionId,
        scenarioId,
        step,
        phase,
        started,
        result.getStatus(),
        result.getError(),
        events);
    return result;
  }

  StepResult skipped(
      String runId,
      ExecutionId executionId,
      ScenarioId scenarioId,
      CompiledStep step,
      StepPhase phase,
      ExecutionEventDispatcher events,
      String reason) {
    events.publish(
        new StepStarted(new RunId(runId), executionId, scenarioId, step.id(), step.type(), phase));
    StepResult result = result(step, clock.instant(), ResultStatus.SKIPPED, reason, Map.of());
    events.publish(
        new StepFinished(
            new RunId(runId),
            executionId,
            scenarioId,
            step.id(),
            step.type(),
            phase,
            result.getStatus(),
            Duration.ZERO,
            reason));
    return result;
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private StepExecutionResult executeHandler(
      ExecutableStep step,
      String runId,
      ExecutionId executionId,
      ScenarioId scenarioId,
      StepRuntimeServices services)
      throws Exception {
    StepHandler handler = (StepHandler) plugins.handler((Class) step.getClass()).orElseThrow();
    return (StepExecutionResult)
        handler.execute(
            step,
            new StepExecutionContext(runId, executionId.value(), scenarioId.value(), services));
  }

  private StepResult result(CompiledStep step, Instant started, StepExecutionResult execution) {
    ResultStatus status =
        execution.status() == StepExecutionStatus.PASS ? ResultStatus.PASS : ResultStatus.FAIL;
    return result(step, started, status, execution.message(), execution.evidence());
  }

  private StepResult result(
      CompiledStep step,
      Instant started,
      ResultStatus status,
      String message,
      Map<String, Object> evidence) {
    StepResult result = new StepResult();
    result.setId(step.id().value());
    result.setName(step.displayName());
    result.setDescription(step.description());
    result.setType(step.type());
    result.setStatus(status);
    result.setStartedAt(started);
    Instant ended = clock.instant();
    result.setEndedAt(ended);
    result.setDurationMs(Duration.between(started, ended).toMillis());
    result.setError(message);
    result.setEvidence(new LinkedHashMap<>(evidence));
    return result;
  }

  private void publishFinished(
      String runId,
      ExecutionId executionId,
      ScenarioId scenarioId,
      CompiledStep step,
      StepPhase phase,
      Instant started,
      ResultStatus status,
      String message,
      ExecutionEventDispatcher events) {
    events.publish(
        new StepFinished(
            new RunId(runId),
            executionId,
            scenarioId,
            step.id(),
            step.type(),
            phase,
            status,
            Duration.between(started, clock.instant()),
            message));
  }

  static String failureMessage(Throwable failure) {
    String message = failure.getMessage();
    return message == null || message.isBlank()
        ? failure.getClass().getSimpleName()
        : failure.getClass().getSimpleName() + ": " + message;
  }
}
