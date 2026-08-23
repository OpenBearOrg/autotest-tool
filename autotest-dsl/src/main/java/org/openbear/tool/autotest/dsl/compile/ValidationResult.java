package org.openbear.tool.autotest.dsl.compile;

import java.util.List;
import java.util.Objects;
import org.openbear.tool.autotest.core.config.ProjectConfig;
import org.openbear.tool.autotest.core.domain.CompiledEnvironment;
import org.openbear.tool.autotest.core.domain.CompiledScenario;

public record ValidationResult(
    ProjectConfig project,
    List<CompiledScenario> scenarios,
    int suiteCount,
    List<CompiledEnvironment> environments,
    List<ValidationMessage> messages) {
  public ValidationResult {
    project = Objects.requireNonNull(project, "project");
    scenarios = scenarios == null ? List.of() : List.copyOf(scenarios);
    environments = environments == null ? List.of() : List.copyOf(environments);
    messages = messages == null ? List.of() : List.copyOf(messages);
    if (suiteCount < 0) throw new IllegalArgumentException("suiteCount must be >= 0");
  }

  public boolean valid() {
    return messages.stream().noneMatch(ValidationMessage::isError);
  }
}
