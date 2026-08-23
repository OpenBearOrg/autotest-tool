package org.openbear.tool.autotest.core.engine;

import java.time.Clock;
import org.openbear.tool.autotest.core.domain.ExecutionPlan;

@FunctionalInterface
public interface EngineRuntimeFactory {
  EngineRuntime open(ExecutionPlan plan, Clock clock);
}
