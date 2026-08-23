package org.openbear.tool.autotest.cli;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Callable;
import org.openbear.tool.autotest.core.ExitCode;
import org.openbear.tool.autotest.core.domain.CompiledEnvironment;
import org.openbear.tool.autotest.core.domain.CompiledEnvironmentView;
import org.openbear.tool.autotest.core.domain.EnvironmentId;
import org.openbear.tool.autotest.core.domain.PollingSettings;
import org.openbear.tool.autotest.core.model.ComparisonResult;
import org.openbear.tool.autotest.core.report.ComparisonCalculator;
import org.openbear.tool.autotest.core.secret.EnvironmentSecretProvider;
import org.openbear.tool.autotest.reporting.DefaultComparisonReportWriter;
import org.openbear.tool.autotest.spi.plugin.PluginRuntimeContext;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "compare", description = "Compare baseline and candidate run.json files")
public class CompareCommand implements Callable<Integer> {
  @Option(names = "--baseline", required = true)
  Path baseline;

  @Option(names = "--candidate", required = true)
  Path candidate;

  @Option(names = "--out", defaultValue = "comparison-report")
  Path out;

  @Override
  public Integer call() throws Exception {
    ComparisonCalculator calculator = new ComparisonCalculator();
    ComparisonResult r = calculator.compare(baseline, candidate);
    try (var plugins = PluginBootstrap.dynamicRegistry()) {
      plugins.open(
          new PluginRuntimeContext(
              new CompiledEnvironmentView(
                  new CompiledEnvironment(
                      new EnvironmentId("compare"),
                      Map.of(),
                      Map.of(),
                      Map.of(),
                      false,
                      new PollingSettings(Duration.ofSeconds(1), Duration.ofMillis(1)))),
              new PublicEngineRuntimeFactory.WorkspaceResources(
                  new org.openbear.tool.autotest.core.util.Workspace(Path.of("."))),
              new EnvironmentSecretProvider()::resolve,
              Clock.systemUTC()));
      plugins.capabilities().all(DefaultComparisonReportWriter.class).stream()
          .findFirst()
          .orElseThrow(
              () -> new IllegalStateException("No public comparison reporting plugin is available"))
          .write(r, out);
    }
    System.out.println(calculator.console(r));
    System.out.println("Report: " + out.toAbsolutePath());
    return r.isRegression() ? ExitCode.TEST_FAILURE : 0;
  }
}
