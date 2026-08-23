package org.openbear.tool.autotest.core.plugin.builtin;

import org.openbear.tool.autotest.core.engine.VariableResolver;
import org.openbear.tool.autotest.spi.step.StepExecutionContext;

final class PublicStepValues {
  private PublicStepValues() {}

  static Object resolve(Object value, StepExecutionContext context) {
    return VariableResolver.resolve(value, context.services().variables());
  }
}
