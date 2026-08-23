package org.openbear.tool.autotest.jdbc.runtime;

/** Indicates that a configured database requires a JDBC driver unavailable to the running JVM. */
public final class JdbcDriverUnavailableException extends RuntimeException {
  public JdbcDriverUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
