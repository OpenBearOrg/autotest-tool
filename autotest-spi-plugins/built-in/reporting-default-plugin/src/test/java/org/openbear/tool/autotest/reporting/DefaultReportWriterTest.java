package org.openbear.tool.autotest.reporting;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openbear.tool.autotest.core.config.ReportingConfig;
import org.openbear.tool.autotest.core.model.ResultStatus;
import org.openbear.tool.autotest.core.model.RunResult;
import org.openbear.tool.autotest.core.model.ScenarioResult;

class DefaultReportWriterTest {
  @TempDir Path temp;

  @Test
  void writesCompatibleSelfContainedRunReport() throws Exception {
    RunResult run = new RunResult();
    run.setProject("project");
    run.setRunId("run-1");
    run.setEnvironment("test");
    run.setStatus(ResultStatus.PASS);
    ScenarioResult scenario = new ScenarioResult();
    scenario.setId("SCENARIO-1");
    scenario.setName("Scenario one");
    scenario.setStatus(ResultStatus.PASS);
    run.getScenarios().add(scenario);

    Path output =
        new DefaultRunReportWriter().write(run, temp.resolve("report"), new ReportingConfig());
    assertTrue(Files.exists(output.resolve("run.json")));
    assertTrue(Files.exists(output.resolve("junit.xml")));
    assertTrue(Files.exists(output.resolve("index.html")));
    assertTrue(Files.exists(output.resolve("scenarios/SCENARIO-1.html")));
    String html = Files.readString(output.resolve("index.html"));
    assertTrue(html.contains("--pass"));
    assertTrue(html.contains("data-report-search"));
    assertTrue(html.contains("addEventListener"));
  }
}
