package org.openbear.tool.autotest.core.domain;

public enum ScenarioExecutionPolicy {
  SEQUENTIAL,
  PARALLEL_SAFE;

  public boolean isParallelSafe() {
    return this == PARALLEL_SAFE;
  }

  public static ScenarioExecutionPolicy fromDsl(String isolation) {
    if (isolation == null || isolation.isBlank() || "sequential".equalsIgnoreCase(isolation))
      return SEQUENTIAL;
    if ("parallel-safe".equalsIgnoreCase(isolation)) return PARALLEL_SAFE;
    throw new IllegalArgumentException("Unsupported scenario isolation: " + isolation);
  }
}
