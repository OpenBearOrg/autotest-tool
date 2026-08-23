package org.openbear.tool.autotest.cli;

final class LoggingSupport {
  static final String LOG_LEVEL_PROPERTY = "AUTOTEST_LOG_LEVEL";

  private LoggingSupport() {}

  static void configure(LogLevel level) {
    System.setProperty(LOG_LEVEL_PROPERTY, level.name());
  }
}
