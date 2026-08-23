package org.openbear.tool.autotest.core.scheduler;

import org.openbear.tool.autotest.core.model.ScenarioResult;

@FunctionalInterface
public interface ScenarioExecutionFunction<T> {
  ScenarioResult execute(T scenario);
}
