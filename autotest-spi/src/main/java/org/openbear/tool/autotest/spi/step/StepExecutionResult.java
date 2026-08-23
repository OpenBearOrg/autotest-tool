package org.openbear.tool.autotest.spi.step;

import java.util.Map;
import java.util.Objects;

public record StepExecutionResult(
    StepExecutionStatus status,
    String message,
    Map<String, Object> captures,
    Map<String, Object> evidence) {
  public StepExecutionResult {
    Objects.requireNonNull(status, "status");
    captures = captures == null ? Map.of() : Map.copyOf(captures);
    evidence = evidence == null ? Map.of() : Map.copyOf(evidence);
  }

  public static StepExecutionResult success(
      Map<String, Object> captures, Map<String, Object> evidence) {
    return new StepExecutionResult(StepExecutionStatus.PASS, null, captures, evidence);
  }

  public static StepExecutionResult failure(String message, Map<String, Object> evidence) {
    return new StepExecutionResult(StepExecutionStatus.FAIL, message, Map.of(), evidence);
  }
}
