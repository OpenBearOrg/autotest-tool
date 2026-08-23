package org.openbear.tool.autotest.core.domain;

import java.util.Objects;

final class IdValue {
  private IdValue() {}

  static String requireNonBlank(String value, String field) {
    String normalized = Objects.requireNonNull(value, field).trim();
    if (normalized.isEmpty()) throw new IllegalArgumentException(field + " must not be blank");
    return normalized;
  }
}
