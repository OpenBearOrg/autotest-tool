package org.openbear.tool.autotest.core.domain;

public record ExecutionId(String value) {
  public ExecutionId {
    value = IdValue.requireNonBlank(value, "executionId");
  }
}
