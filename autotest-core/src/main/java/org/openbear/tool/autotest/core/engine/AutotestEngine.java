package org.openbear.tool.autotest.core.engine;

import org.openbear.tool.autotest.core.domain.ExecutionPlan;
import org.openbear.tool.autotest.core.model.RunResult;

public interface AutotestEngine extends AutoCloseable {
  RunResult run(ExecutionPlan plan, RunRequest request);

  @Override
  void close();
}
