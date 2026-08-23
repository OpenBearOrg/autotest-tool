package org.openbear.tool.autotest.core.engine;

import org.openbear.tool.autotest.core.domain.ExecutionPlan;
import org.openbear.tool.autotest.core.model.RunResult;

public interface EngineRuntime extends AutoCloseable {
  RunResult execute(ExecutionPlan plan, RunRequest request);

  @Override
  void close();
}
