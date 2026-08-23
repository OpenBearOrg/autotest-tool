package org.openbear.tool.autotest.core.domain;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record CompiledScenario(
    ScenarioId id,
    String name,
    Set<String> tags,
    Set<String> requiredVariables,
    Map<String, Object> variables,
    List<CompiledStep> setup,
    List<CompiledStep> steps,
    List<CompiledStep> cleanup,
    ScenarioExecutionPolicy executionPolicy,
    ScenarioSource source) {
  public CompiledScenario {
    Objects.requireNonNull(id, "id");
    name = IdValue.requireNonBlank(name, "name");
    tags = ImmutableValues.set(tags);
    requiredVariables = ImmutableValues.set(requiredVariables);
    variables = ImmutableValues.map(variables);
    setup = ImmutableValues.list(setup);
    steps = ImmutableValues.list(steps);
    cleanup = ImmutableValues.list(cleanup);
    executionPolicy = Objects.requireNonNull(executionPolicy, "executionPolicy");
    source = Objects.requireNonNull(source, "source");
  }
}
