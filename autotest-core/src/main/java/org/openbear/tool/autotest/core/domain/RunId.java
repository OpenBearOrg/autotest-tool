package org.openbear.tool.autotest.core.domain;

public record RunId(String value) {
  public RunId {
    value = IdValue.requireNonBlank(value, "runId");
  }
}
