package org.openbear.tool.autotest.core.engine;

import java.time.Clock;
import java.util.Objects;
import org.openbear.tool.autotest.core.domain.ExecutionPlan;
import org.openbear.tool.autotest.core.model.RunResult;

public final class DefaultAutotestEngine implements AutotestEngine {
  private final EngineRuntimeFactory runtimeFactory;
  private final Clock clock;

  public DefaultAutotestEngine(EngineRuntimeFactory runtimeFactory, Clock clock) {
    this.runtimeFactory = Objects.requireNonNull(runtimeFactory, "runtimeFactory");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  @Override
  public RunResult run(ExecutionPlan plan, RunRequest request) {
    Objects.requireNonNull(plan, "plan");
    Objects.requireNonNull(request, "request");
    try (EngineRuntime runtime = runtimeFactory.open(plan, clock)) {
      return runtime.execute(plan, request);
    }
  }

  @Override
  public void close() {
    // Runtime resources are scoped to each run and closed by run(...).
  }
}
