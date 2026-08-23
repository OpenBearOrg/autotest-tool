package org.openbear.tool.autotest.core.plugin.builtin;

import java.util.LinkedHashMap;
import java.util.Map;
import org.openbear.tool.autotest.spi.step.StepConfiguration;
import org.openbear.tool.autotest.spi.step.StepIdentity;

public record AssertConfiguration(
    String id,
    String name,
    String description,
    boolean continueOnFailure,
    Map<String, Object> values)
    implements StepConfiguration {
  public AssertConfiguration {
    values = values == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(values));
  }

  @Override
  public StepIdentity identity() {
    return new StepIdentity(id, name, description, continueOnFailure);
  }
}
