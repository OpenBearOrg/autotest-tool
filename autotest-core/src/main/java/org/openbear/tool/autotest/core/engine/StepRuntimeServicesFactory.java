package org.openbear.tool.autotest.core.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.openbear.tool.autotest.core.assertion.AssertionEngine;
import org.openbear.tool.autotest.core.assertion.AssertionFailureException;
import org.openbear.tool.autotest.core.domain.CompiledEnvironment;
import org.openbear.tool.autotest.core.domain.CompiledEnvironmentView;
import org.openbear.tool.autotest.core.json.JsonPathSupport;
import org.openbear.tool.autotest.core.secret.EnvironmentSecretProvider;
import org.openbear.tool.autotest.core.util.Workspace;
import org.openbear.tool.autotest.spi.service.PollRequest;
import org.openbear.tool.autotest.spi.service.PollResult;
import org.openbear.tool.autotest.spi.step.StepRuntimeServices;

/** Creates public SPI runtime services for a single scenario's variables. */
final class StepRuntimeServicesFactory {
  private final CompiledEnvironment environment;
  private final Workspace workspace;
  private final Clock clock;
  private final ObjectMapper mapper;
  private final JsonPathSupport json;
  private final AssertionEngine assertions;
  private final PollingEngine polling;
  private final EnvironmentSecretProvider secrets;

  StepRuntimeServicesFactory(CompiledEnvironment environment, Workspace workspace, Clock clock) {
    this.environment = Objects.requireNonNull(environment, "environment");
    this.workspace = Objects.requireNonNull(workspace, "workspace");
    this.clock = Objects.requireNonNull(clock, "clock");
    mapper =
        new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    json = new JsonPathSupport(mapper);
    assertions = new AssertionEngine();
    polling = new PollingEngine(clock);
    secrets = new EnvironmentSecretProvider();
  }

  StepRuntimeServices create(ScenarioVariables variables) {
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

  private final class SpiAssertions implements org.openbear.tool.autotest.spi.service.Assertions {
    private final ScenarioVariables variables;

    private SpiAssertions(ScenarioVariables variables) {
      this.variables = variables;
    }

    @Override
    public void verify(Object actual, Object expected) {
      String failure = assertions.compare(actual, expected);
      if (failure != null) throw new AssertionFailureException(failure);
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
            } catch (RuntimeException failure) {
              failures.add(path + ": " + failure.getMessage());
            }
          });
      if (!failures.isEmpty()) throw new AssertionFailureException(String.join("; ", failures));
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
