package org.openbear.tool.autotest.core.plugin.builtin;

import java.util.LinkedHashMap;
import java.util.Map;
import org.openbear.tool.autotest.spi.step.StepExecutionContext;
import org.openbear.tool.autotest.spi.step.StepExecutionResult;
import org.openbear.tool.autotest.spi.step.StepHandler;

final class PublicAssertStepHandler implements StepHandler<AssertExecutableStep> {
  @Override
  public Class<AssertExecutableStep> stepType() {
    return AssertExecutableStep.class;
  }

  @Override
  public StepExecutionResult execute(AssertExecutableStep step, StepExecutionContext context) {
    Map<String, Object> observed = new LinkedHashMap<>();
    try {
      for (Map.Entry<String, Object> entry : step.values().entrySet()) {
        Object actual = PublicStepValues.resolve(entry.getKey(), context);
        Object expected = PublicStepValues.resolve(entry.getValue(), context);
        observed.put(entry.getKey(), actual);
        context.services().assertions().verify(actual, expected);
      }
      return StepExecutionResult.success(Map.of(), Map.of("observed", observed));
    } catch (RuntimeException failure) {
      return StepExecutionResult.failure(failure.getMessage(), Map.of("observed", observed));
    }
  }
}
