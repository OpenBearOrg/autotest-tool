package org.openbear.tool.autotest.core.domain;

public record SecretReference(String value) {
  public SecretReference {
    value = IdValue.requireNonBlank(value, "secretReference");
  }
}
