package org.openbear.tool.autotest.core.domain;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record ExecutionPlan(
    String projectName,
    CompiledEnvironment environment,
    List<CompiledScenario> scenarios,
    Map<String, Object> commonVariables,
    ExecutionSettings settings,
    Set<PluginRequirement> pluginRequirements,
    CompilationMetadata metadata) {
  public ExecutionPlan {
    projectName = IdValue.requireNonBlank(projectName, "projectName");
    environment = Objects.requireNonNull(environment, "environment");
    scenarios = ImmutableValues.list(scenarios);
    commonVariables = ImmutableValues.map(commonVariables);
    settings = Objects.requireNonNull(settings, "settings");
    pluginRequirements = ImmutableValues.set(pluginRequirements);
    metadata = Objects.requireNonNull(metadata, "metadata");
    if (scenarios.isEmpty()) throw new IllegalArgumentException("ExecutionPlan requires scenarios");
  }
}
