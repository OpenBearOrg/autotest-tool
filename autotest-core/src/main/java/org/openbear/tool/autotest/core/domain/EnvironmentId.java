package org.openbear.tool.autotest.core.domain;

public record EnvironmentId(String value) {
  public EnvironmentId {
    value = IdValue.requireNonBlank(value, "environmentId");
  }
}
