package org.openbear.tool.autotest.core.assertion;

/** Signals an ordinary assertion mismatch reported through the public step SPI. */
public final class AssertionFailureException extends RuntimeException {
  public AssertionFailureException(String message) {
    super(message);
  }

  public AssertionFailureException(String message, Throwable cause) {
    super(message, cause);
  }
}
