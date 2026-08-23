package org.openbear.tool.autotest.core.plugin.builtin;

import java.util.Map;
import org.openbear.tool.autotest.spi.step.ExecutableStep;
import org.openbear.tool.autotest.spi.step.StepIdentity;

public record AssertExecutableStep(StepIdentity identity, Map<String, Object> values)
    implements ExecutableStep {
  public AssertExecutableStep {
    values = values == null ? Map.of() : Map.copyOf(values);
  }

  @Override
  public String type() {
    return "assert";
  }
}
