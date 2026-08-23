package org.openbear.tool.autotest.cli;

import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import org.openbear.tool.autotest.core.ExitCode;
import org.openbear.tool.autotest.core.config.ProjectConfig;
import org.openbear.tool.autotest.core.domain.RunId;
import org.openbear.tool.autotest.core.engine.AutotestEngine;
import org.openbear.tool.autotest.core.engine.DefaultAutotestEngine;
import org.openbear.tool.autotest.core.engine.DefaultIdGenerator;
import org.openbear.tool.autotest.core.engine.RunRequest;
import org.openbear.tool.autotest.core.event.ConsoleExecutionListener;
import org.openbear.tool.autotest.core.model.ResultStatus;
import org.openbear.tool.autotest.core.model.RunResult;
import org.openbear.tool.autotest.core.model.ScenarioResult;
import org.openbear.tool.autotest.core.util.Workspace;
import org.openbear.tool.autotest.dsl.ConfigLoader;
import org.openbear.tool.autotest.dsl.compile.CompileRequest;
import org.openbear.tool.autotest.dsl.compile.WorkspaceCompiler;
import org.openbear.tool.autotest.reporting.DefaultRunReportWriter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "run", description = "Execute scenarios or suites")
public class RunCommand implements Callable<Integer> {
  @Option(names = "--workspace", defaultValue = ".")
  Path workspacePath;

  @Option(names = "--env")
  String envName;

  @Option(names = "--scenario", description = "Scenario id/path; repeatable")
  List<String> scenarioRefs = new ArrayList<>();

  @Option(names = "--suite", description = "Suite id/path")
  String suiteRef;

  @Option(names = "--tag", description = "Include scenarios with tag; repeatable")
  List<String> tags = new ArrayList<>();

  @Option(names = "--var", description = "Runtime variable name=value; repeatable")
  List<String> vars = new ArrayList<>();

  @Option(names = "--label")
  String label;

  @Option(names = "--parallel")
  Integer parallel;

  @Option(names = "--fail-fast")
  Boolean failFast;

  @Option(names = "--skip-doctor")
  boolean skipDoctor;

  @Option(names = "--report-dir")
  String reportDir;

  @Option(names = "--plugin-dir", description = "External plugin directory; repeatable")
  List<Path> pluginDirs = new ArrayList<>();

  private static void print(RunResult r, Path out) {
    System.out.println();
    for (ScenarioResult s : r.getScenarios())
      System.out.printf("%-34s %-5s %8d ms%n", s.getId(), s.getStatus(), s.getDurationMs());
    System.out.printf(
        "%nRESULT: %s  scenarios=%d  duration=%d ms%nReport: %s%n",
        r.getStatus(), r.getScenarios().size(), r.getDurationMs(), out.toAbsolutePath());
  }

  @Override
  public Integer call() throws Exception {
    Workspace w = CliSupport.workspace(workspacePath);
    ConfigLoader loader = new ConfigLoader(w);
    ProjectConfig project = loader.loadProject();
    return runPublicSpi(w, project);
  }

  private Integer runPublicSpi(Workspace workspace, ProjectConfig project) throws Exception {
    Clock clock = Clock.systemUTC();
    try (org.openbear.tool.autotest.core.plugin.PluginRegistry plugins =
        PluginBootstrap.dynamicRegistry(resolvePluginDirs(workspace))) {
      var executionPlan =
          new WorkspaceCompiler(plugins, clock)
              .compile(
                  new CompileRequest(
                      workspace.root(),
                      envName,
                      scenarioRefs,
                      suiteRef,
                      Set.copyOf(tags),
                      CliSupport.parseVars(vars),
                      parallel,
                      failFast,
                      skipDoctor));
      String runId = new DefaultIdGenerator(clock).nextRunId().value();
      Path out = CliSupport.reportDir(workspace, project, reportDir, label, runId);
      try (AutotestEngine engine =
          new DefaultAutotestEngine(new PublicEngineRuntimeFactory(workspace, plugins), clock)) {
        RunResult result =
            engine.run(
                executionPlan,
                new RunRequest(new RunId(runId), label, List.of(new ConsoleExecutionListener())));
        plugins.capabilities().all(DefaultRunReportWriter.class).stream()
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No public reporting plugin is available"))
            .write(result, out, project.getReporting());
        print(result, out);
        return result.getStatus() == ResultStatus.PASS ? 0 : ExitCode.TEST_FAILURE;
      }
    }
  }

  private List<Path> resolvePluginDirs(Workspace workspace) {
    return pluginDirs.stream()
        .map(path -> path.isAbsolute() ? path : workspace.root().resolve(path))
        .toList();
  }
}
