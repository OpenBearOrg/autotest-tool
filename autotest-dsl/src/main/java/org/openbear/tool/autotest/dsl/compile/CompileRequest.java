package org.openbear.tool.autotest.dsl.compile;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record CompileRequest(
    Path workspace,
    String environment,
    List<String> scenarios,
    String suite,
    Set<String> includeTags,
    Map<String, Object> runtimeVariables,
    Integer parallelismOverride,
    Boolean failFastOverride,
    boolean skipDoctor) {
  public CompileRequest {
    workspace = Objects.requireNonNull(workspace, "workspace").toAbsolutePath().normalize();
    scenarios = scenarios == null ? List.of() : List.copyOf(scenarios);
    includeTags = includeTags == null ? Set.of() : Set.copyOf(includeTags);
    runtimeVariables = runtimeVariables == null ? Map.of() : Map.copyOf(runtimeVariables);
    if (parallelismOverride != null && parallelismOverride < 1)
      throw new IllegalArgumentException("parallelismOverride must be >= 1");
  }
}
