package org.openbear.tool.autotest.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

class LoggingSupportTest {
  private String previousLogLevel;

  @BeforeEach
  void captureLogLevelProperty() {
    previousLogLevel = System.getProperty(LoggingSupport.LOG_LEVEL_PROPERTY);
  }

  @AfterEach
  void restoreLogLevelProperty() {
    if (previousLogLevel == null) System.clearProperty(LoggingSupport.LOG_LEVEL_PROPERTY);
    else System.setProperty(LoggingSupport.LOG_LEVEL_PROPERTY, previousLogLevel);
  }

  @Test
  void defaultsToInfo() {
    AutotestCli application = new AutotestCli();
    new CommandLine(application).parseArgs();

    assertEquals(LogLevel.INFO, application.logLevel());
  }

  @Test
  void logLevelCanBePassedAfterTheSubcommand() {
    AutotestCli application = new AutotestCli();
    CommandLine commandLine = new CommandLine(application);
    commandLine.setExecutionStrategy(parseResult -> 0);

    assertEquals(0, commandLine.execute("version", "--log-level", "DEBUG"));
    assertEquals(LogLevel.DEBUG, application.logLevel());
  }

  @Test
  void configurePublishesLevelForLogback() {
    LoggingSupport.configure(LogLevel.ERROR);

    assertEquals("ERROR", System.getProperty(LoggingSupport.LOG_LEVEL_PROPERTY));
  }
}
