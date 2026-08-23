package org.openbear.tool.autotest.core.domain;

public record ResourceId(String value) {
  public ResourceId {
    value = IdValue.requireNonBlank(value, "resourceId");
  }
}
