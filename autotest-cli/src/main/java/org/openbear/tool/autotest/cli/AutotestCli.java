package org.openbear.tool.autotest.cli;

import org.openbear.tool.autotest.core.AutotestVersion;
import org.openbear.tool.autotest.core.ExitCode;
import org.openbear.tool.autotest.dsl.ValidationException;
import org.openbear.tool.autotest.jdbc.runtime.JdbcDriverUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ScopeType;

@Command(
    name = "autotest-tool",
    mixinStandardHelpOptions = true,
    description = "Autotest API/database/message regression test platform",
    subcommands = {
      InitCommand.class,
      ValidateCommand.class,
      DoctorCommand.class,
      ListCommand.class,
      RunCommand.class,
      CompareCommand.class,
      VersionCommand.class
    })
public class AutotestCli implements Runnable {
  @Option(
      names = "--log-level",
      scope = ScopeType.INHERIT,
      defaultValue = "INFO",
      description = "Diagnostic log level: ${COMPLETION-CANDIDATES} (default: ${DEFAULT-VALUE})")
  private LogLevel logLevel = LogLevel.INFO;

  public static void main(String[] args) {
    CommandLine cli = new CommandLine(new AutotestCli());
    AutotestCli root = cli.getCommand();
    cli.setExecutionStrategy(
        parseResult -> {
          LoggingSupport.configure(root.logLevel);
          return new CommandLine.RunLast().execute(parseResult);
        });
    cli.setExecutionExceptionHandler(
        (ex, cmd, parse) -> {
          Logger logger = LoggerFactory.getLogger(AutotestCli.class);
          if (ex instanceof ValidationException) {
            logger.error("Command validation failed: {}", ex.getMessage());
            return ExitCode.DSL_VALIDATION_ERROR;
          }
          if (ex instanceof EnvironmentFailureException
              || ex instanceof JdbcDriverUnavailableException) {
            logger.error("Environment check failed: {}", ex.getMessage());
            return ExitCode.ENVIRONMENT_ERROR;
          }
          if (ex instanceof IllegalArgumentException) {
            logger.error("Command configuration failed: {}", ex.getMessage());
            return ExitCode.CONFIGURATION_ERROR;
          }
          logger.error("Command failed", ex);
          return ExitCode.INTERNAL_ERROR;
        });
    System.exit(cli.execute(args));
  }

  @Override
  public void run() {
    System.out.println("Autotest " + AutotestVersion.VERSION + ". Use --help or a subcommand.");
  }

  LogLevel logLevel() {
    return logLevel;
  }
}
