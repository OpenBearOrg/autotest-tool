package org.openbear.tool.autotest.core.plugin.builtin;

import java.util.LinkedHashMap;
import java.util.Map;
import org.openbear.tool.autotest.spi.step.StepExecutionContext;
import org.openbear.tool.autotest.spi.step.StepExecutionResult;
import org.openbear.tool.autotest.spi.step.StepHandler;

final class PublicSetStepHandler implements StepHandler<SetExecutableStep> {
  @Override
  public Class<SetExecutableStep> stepType() {
    return SetExecutableStep.class;
  }

  @Override
  public StepExecutionResult execute(SetExecutableStep step, StepExecutionContext context) {
    Map<String, Object> values = new LinkedHashMap<>();
    step.values()
        .forEach(
            (name, value) -> {
              Object resolved = PublicStepValues.resolve(value, context);
              context.services().variables().put(name, resolved);
              values.put(name, resolved);
            });
    return StepExecutionResult.success(Map.of(), Map.of("set", values));
  }
}
