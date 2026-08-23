package org.openbear.tool.autotest.core.domain;

import java.util.Map;
import java.util.Objects;
import org.openbear.tool.autotest.spi.step.ExecutableStep;

public record CompiledStep(
    StepId id,
    String name,
    String description,
    boolean continueOnFailure,
    String type,
    Map<String, Object> configuration,
    ExecutableStep executable) {
  public CompiledStep(
      StepId id,
      String name,
      String description,
      boolean continueOnFailure,
      String type,
      Map<String, Object> configuration) {
    this(id, name, description, continueOnFailure, type, configuration, null);
  }

  public CompiledStep {
    Objects.requireNonNull(id, "id");
    type = IdValue.requireNonBlank(type, "stepType");
    configuration = ImmutableValues.map(configuration);
    if (executable != null && !type.equals(executable.type()))
      throw new IllegalArgumentException("Executable step type does not match compiled type");
  }

  public String displayName() {
    return name == null || name.isBlank() ? id.value() : name;
  }
}
