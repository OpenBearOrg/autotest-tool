package org.openbear.tool.autotest.core.domain;

public record StepId(String value) {
  public StepId {
    value = IdValue.requireNonBlank(value, "stepId");
  }
}
