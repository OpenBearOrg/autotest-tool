package org.openbear.tool.autotest.core.scheduler;

import java.util.List;
import org.openbear.tool.autotest.core.domain.RunId;
import org.openbear.tool.autotest.core.model.ScenarioResult;

@FunctionalInterface
public interface ScenarioScheduler {
  <T> List<ScenarioResult> execute(
      RunId runId,
      List<T> scenarios,
      ScenarioExecutionFunction<T> execution,
      SchedulingOptions options);
}
