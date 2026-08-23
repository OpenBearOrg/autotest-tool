package org.openbear.tool.autotest.core.engine;

import org.openbear.tool.autotest.core.domain.ExecutionId;
import org.openbear.tool.autotest.core.domain.RunId;
import org.openbear.tool.autotest.core.domain.ScenarioId;

public interface IdGenerator {
  RunId nextRunId();

  ExecutionId nextExecutionId(ScenarioId scenarioId);
}
