package org.openbear.tool.autotest.core.engine;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.openbear.tool.autotest.core.AutotestVersion;
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
import org.openbear.tool.autotest.core.model.RunResult;

class DefaultAutotestEngineTest {
  @Test
  void delegatesTheCompiledPlanAndClosesTheRuntime() {
    ExecutionPlan plan = plan();
    RunRequest request = RunRequest.create(new RunId("run-1"), "label");
    RunResult expected = new RunResult();
    boolean[] closed = {false};
    ExecutionPlan[] receivedPlan = {null};

    AutotestEngine engine =
        new DefaultAutotestEngine(
            (received, ignored) -> {
              receivedPlan[0] = received;
              return new EngineRuntime() {
                @Override
                public RunResult execute(ExecutionPlan ignoredPlan, RunRequest ignoredRequest) {
                  return expected;
                }

                @Override
                public void close() {
                  closed[0] = true;
                }
              };
            },
            Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));

    assertSame(expected, engine.run(plan, request));
    assertSame(plan, receivedPlan[0]);
    assertTrue(closed[0]);
  }

  private static ExecutionPlan plan() {
    var scenario =
        new CompiledScenario(
            new ScenarioId("scenario-1"),
            "Scenario",
            Set.of(),
            Set.of(),
            Map.of(),
            List.of(),
            List.of(),
            List.of(),
            ScenarioExecutionPolicy.SEQUENTIAL,
            new ScenarioSource(Path.of("scenario.yaml"), "checksum", Map.of()));
    var environment =
        new CompiledEnvironment(
            new EnvironmentId("local"),
            Map.of(),
            Map.of(),
            Map.of(),
            false,
            new PollingSettings(Duration.ofSeconds(1), Duration.ofMillis(100)));
    return new ExecutionPlan(
        "project",
        environment,
        List.of(scenario),
        Map.of(),
        new ExecutionSettings(1, false, true),
        Set.of(),
        new CompilationMetadata(AutotestVersion.DSL_VERSION, null, Instant.EPOCH));
  }
}
