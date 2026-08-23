package org.openbear.tool.autotest.reporting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openbear.tool.autotest.core.config.ReportingConfig;
import org.openbear.tool.autotest.core.model.ResultStatus;
import org.openbear.tool.autotest.core.model.RunResult;
import org.openbear.tool.autotest.core.model.ScenarioResult;

class ResultReporterTest {
  @TempDir Path temp;

  @Test
  void writesVersionedJsonAndBackwardCompatibleAlias() throws Exception {
    RunResult run = run(ResultStatus.PASS, "SCENARIO-1");

    new JsonResultReporter().write(run, temp, new ReportingOptions(new ReportingConfig()));

    JsonNode result = new ObjectMapper().readTree(temp.resolve("result.json").toFile());
    JsonNode legacy = new ObjectMapper().readTree(temp.resolve("run.json").toFile());
    assertEquals("1.0", result.get("schemaVersion").asText());
    assertEquals(result, legacy);
  }

  @Test
  void writesEscapedJUnitXmlForFailure() throws Exception {
    RunResult run = run(ResultStatus.FAIL, "scenario<&");
    ScenarioResult scenario = run.getScenarios().getFirst();
    scenario.setError("expected <PASS> & got FAIL");

    new JUnitXmlResultReporter().write(run, temp, new ReportingOptions(new ReportingConfig()));

    var document =
        DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(temp.resolve("junit.xml").toFile());
    assertEquals("testsuite", document.getDocumentElement().getTagName());
    assertTrue(document.getElementsByTagName("failure").getLength() == 1);
    assertEquals(
        "scenario<&",
        document
            .getElementsByTagName("testcase")
            .item(0)
            .getAttributes()
            .getNamedItem("name")
            .getNodeValue());
    assertTrue(Files.size(temp.resolve("junit.xml")) > 0);
  }

  private static RunResult run(ResultStatus status, String scenarioId) {
    RunResult run = new RunResult();
    run.setProject("project");
    run.setStatus(status);
    ScenarioResult scenario = new ScenarioResult();
    scenario.setId(scenarioId);
    scenario.setName("Scenario");
    scenario.setStatus(status);
    run.getScenarios().add(scenario);
    return run;
  }
}
