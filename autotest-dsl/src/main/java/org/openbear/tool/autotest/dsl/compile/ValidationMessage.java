package org.openbear.tool.autotest.dsl.compile;

import java.util.Objects;

public record ValidationMessage(ValidationSeverity severity, String location, String message) {
  public ValidationMessage {
    severity = Objects.requireNonNull(severity, "severity");
    location = location == null ? "" : location;
    message = Objects.requireNonNull(message, "message");
  }

  public boolean isError() {
    return severity == ValidationSeverity.ERROR;
  }
}
