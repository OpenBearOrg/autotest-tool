package org.openbear.tool.autotest.cli;

final class EnvironmentFailureException extends RuntimeException {
  EnvironmentFailureException(String message) {
    super(message);
  }
}
