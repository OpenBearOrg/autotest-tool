package org.openbear.tool.autotest.core.plugin.builtin;

import java.util.Map;
import org.openbear.tool.autotest.spi.step.ExecutableStep;
import org.openbear.tool.autotest.spi.step.StepIdentity;

public record SetExecutableStep(StepIdentity identity, Map<String, Object> values)
    implements ExecutableStep {
  public SetExecutableStep {
    values = values == null ? Map.of() : Map.copyOf(values);
  }

  @Override
  public String type() {
    return "set";
  }
}
