package org.openbear.tool.autotest.core.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.openbear.tool.autotest.core.assertion.AssertionEngine;
import org.openbear.tool.autotest.core.domain.CompiledEnvironment;
import org.openbear.tool.autotest.core.domain.CompiledEnvironmentView;
import org.openbear.tool.autotest.core.domain.CompiledScenario;
import org.openbear.tool.autotest.core.domain.CompiledStep;
import org.openbear.tool.autotest.core.domain.ExecutionId;
import org.openbear.tool.autotest.core.domain.RunId;
import org.openbear.tool.autotest.core.domain.ScenarioId;
import org.openbear.tool.autotest.core.event.ExecutionEventDispatcher;
import org.openbear.tool.autotest.core.event.ScenarioFinished;
import org.openbear.tool.autotest.core.event.ScenarioStarted;
import org.openbear.tool.autotest.core.event.StepFinished;
import org.openbear.tool.autotest.core.event.StepPhase;
import org.openbear.tool.autotest.core.event.StepStarted;
import org.openbear.tool.autotest.core.json.JsonPathSupport;
import org.openbear.tool.autotest.core.model.ResultStatus;
import org.openbear.tool.autotest.core.model.ScenarioResult;
import org.openbear.tool.autotest.core.model.StepResult;
import org.openbear.tool.autotest.core.plugin.PluginRegistry;
import org.openbear.tool.autotest.core.secret.EnvironmentSecretProvider;
import org.openbear.tool.autotest.core.util.Workspace;
import org.openbear.tool.autotest.spi.service.PollRequest;
import org.openbear.tool.autotest.spi.service.PollResult;
import org.openbear.tool.autotest.spi.step.ExecutableStep;
import org.openbear.tool.autotest.spi.step.StepExecutionContext;
import org.openbear.tool.autotest.spi.step.StepExecutionResult;
import org.openbear.tool.autotest.spi.step.StepExecutionStatus;
import org.openbear.tool.autotest.spi.step.StepHandler;
import org.openbear.tool.autotest.spi.step.StepRuntimeServices;

/** Executes public-SPI executable steps without translating them to legacy DTOs. */
public final class PublicScenarioRunner {
  private final PluginRegistry plugins;
  private final CompiledEnvironment environment;
  private final Workspace workspace;
  private final Clock clock;
  private final IdGenerator ids;
  private final ObjectMapper mapper;
  private final JsonPathSupport json;
  private final AssertionEngine assertions;
  private final PollingEngine polling;
  private final EnvironmentSecretProvider secrets = new EnvironmentSecretProvider();

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
    this.plugins = Objects.requireNonNull(plugins, "plugins");
    this.environment = Objects.requireNonNull(environment, "environment");
    this.workspace = Objects.requireNonNull(workspace, "workspace");
    this.clock = Objects.requireNonNull(clock, "clock");
    this.ids = Objects.requireNonNull(ids, "ids");
    this.mapper =
        new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    this.json = new JsonPathSupport(mapper);
    this.assertions = new AssertionEngine();
    this.polling = new PollingEngine(clock);
  }

  public ScenarioResult run(
      String runId,
      CompiledScenario scenario,
      Map<String, Object> commonVariables,
      ExecutionEventDispatcher events) {
    Instant started = clock.instant();
    ScenarioId scenarioId = scenario.id();
    ExecutionId executionId = ids.nextExecutionId(scenarioId);
    ScenarioResult result = new ScenarioResult();
    result.setId(scenarioId.value());
    result.setName(scenario.name());
    result.setExecutionId(executionId.value());
    result.setChecksum(scenario.source().scenarioChecksum());
    result.setResourceChecksums(scenario.source().resourceChecksums());
    result.setStartedAt(started);
    events.publish(new ScenarioStarted(new RunId(runId), executionId, scenarioId, scenario.name()));

    ScenarioVariables variables =
        new ScenarioVariables(initialVariables(runId, executionId, scenario, commonVariables));
    variables.materializeRuntimeExpressions();
    StepRuntimeServices services = services(variables);
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
              services,
              events,
              true);
      if (setupOk)
        executePhase(
            runId,
            executionId,
            scenarioId,
            scenario.steps(),
            result.getSteps(),
            services,
            events,
            false);
      else
        for (CompiledStep step : scenario.steps())
          result
              .getSteps()
              .add(
                  skipped(
                      runId,
                      executionId,
                      scenarioId,
                      step,
                      StepPhase.MAIN,
                      events,
                      "setup failed"));
    } catch (Exception failure) {
      result.setError(failure.getClass().getSimpleName() + ": " + failure.getMessage());
    } finally {
      executeCleanup(
          runId,
          executionId,
          scenarioId,
          scenario.cleanup(),
          result.getCleanup(),
          services,
          events);
      result.setFinalVariables(variables.snapshot());
      Instant ended = clock.instant();
      result.setEndedAt(ended);
      result.setDurationMs(Duration.between(started, ended).toMillis());
      boolean failed =
          result.getError() != null
              || failed(result.getSetup())
              || failed(result.getSteps())
              || failed(result.getCleanup());
      result.setStatus(failed ? ResultStatus.FAIL : ResultStatus.PASS);
      events.publish(
          new ScenarioFinished(
              new RunId(runId),
              executionId,
              scenarioId,
              result.getStatus(),
              Duration.ofMillis(result.getDurationMs()),
              result.getError()));
    }
    return result;
  }

  private boolean executePhase(
      String runId,
      ExecutionId executionId,
      ScenarioId scenarioId,
      List<CompiledStep> steps,
      List<StepResult> output,
      StepRuntimeServices services,
      ExecutionEventDispatcher events,
      boolean setup) {
    boolean healthy = true;
    for (CompiledStep step : steps) {
      StepPhase phase = setup ? StepPhase.SETUP : StepPhase.MAIN;
      StepResult result =
          healthy
              ? execute(runId, executionId, scenarioId, step, phase, services, events)
              : skipped(
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
      List<CompiledStep> steps,
      List<StepResult> output,
      StepRuntimeServices services,
      ExecutionEventDispatcher events) {
    for (CompiledStep step : steps)
      output.add(
          execute(runId, executionId, scenarioId, step, StepPhase.CLEANUP, services, events));
  }

  private StepResult execute(
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
    } catch (Exception failure) {
      result =
          result(
              step,
              started,
              ResultStatus.ERROR,
              failure.getClass().getSimpleName() + ": " + failure.getMessage(),
              Map.of());
    }
    events.publish(
        new StepFinished(
            new RunId(runId),
            executionId,
            scenarioId,
            step.id(),
            step.type(),
            phase,
            result.getStatus(),
            Duration.between(started, clock.instant()),
            result.getError()));
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

  private StepResult skipped(
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

  private StepRuntimeServices services(ScenarioVariables variables) {
    return new StepRuntimeServices(
        new CompiledEnvironmentView(environment),
        variables,
        new SpiAssertions(variables),
        new SpiJson(),
        this::poll,
        new WorkspaceResources(),
        secrets::resolve,
        clock);
  }

  private <T> PollResult<T> poll(
      PollRequest request,
      java.util.function.Supplier<T> probe,
      java.util.function.Predicate<T> done) {
    Objects.requireNonNull(request, "request");
    var outcome =
        polling.poll(
            "public-spi", request.timeout(), request.interval(), probe, done, value -> value);
    return new PollResult<>(
        outcome.matched(),
        outcome.lastValue(),
        outcome.observations().stream().map(o -> o.getValue()).toList());
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

  private final class SpiAssertions implements org.openbear.tool.autotest.spi.service.Assertions {
    private final ScenarioVariables variables;

    private SpiAssertions(ScenarioVariables variables) {
      this.variables = variables;
    }

    @Override
    public void verify(Object actual, Object expected) {
      String failure = assertions.compare(actual, expected);
      if (failure != null) throw new AssertionError(failure);
    }

    @Override
    public void verifyValues(Object document, Map<String, ?> expectations) {
      List<String> failures = new ArrayList<>();
      expectations.forEach(
          (path, expected) -> {
            try {
              String resolvedPath = String.valueOf(VariableResolver.resolve(path, variables));
              Object resolvedExpected = VariableResolver.resolve(expected, variables);
              verify(json.read(document, resolvedPath), resolvedExpected);
            } catch (RuntimeException | AssertionError failure) {
              failures.add(path + ": " + failure.getMessage());
            }
          });
      if (!failures.isEmpty()) throw new AssertionError(String.join("; ", failures));
    }
  }

  private final class SpiJson implements org.openbear.tool.autotest.spi.service.JsonAccess {
    @Override
    public java.util.Optional<Object> read(Object document, String path) {
      try {
        return java.util.Optional.ofNullable(json.read(document, path));
      } catch (RuntimeException ignored) {
        return java.util.Optional.empty();
      }
    }

    @Override
    public Object require(Object document, String path) {
      return read(document, path)
          .orElseThrow(() -> new IllegalArgumentException("JSONPath did not match: " + path));
    }

    @Override
    public Object parse(String value) {
      try {
        return mapper.readValue(value, Object.class);
      } catch (IOException failure) {
        throw new IllegalArgumentException("Invalid JSON", failure);
      }
    }

    @Override
    public String write(Object value) {
      try {
        return mapper.writeValueAsString(value);
      } catch (IOException failure) {
        throw new IllegalStateException("Unable to write JSON", failure);
      }
    }
  }

  private final class WorkspaceResources
      implements org.openbear.tool.autotest.spi.service.ResourceAccess {
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
        return java.nio.file.Files.readAllBytes(workspace.resolve(path));
      } catch (IOException failure) {
        throw new IllegalArgumentException("Unable to read resource: " + path, failure);
      }
    }
  }
}
