package org.openbear.tool.autotest.dsl;

import java.util.List;

public class ValidationException extends RuntimeException {
  private final List<String> errors;

  public ValidationException(String message, List<String> errors) {
    super(message + (errors.isEmpty() ? "" : ":\n - " + String.join("\n - ", errors)));
    this.errors = List.copyOf(errors);
  }

  public List<String> errors() {
    return errors;
  }
}
