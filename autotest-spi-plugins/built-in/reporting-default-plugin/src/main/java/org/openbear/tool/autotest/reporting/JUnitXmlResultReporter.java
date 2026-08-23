package org.openbear.tool.autotest.reporting;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.openbear.tool.autotest.core.model.RunResult;
import org.openbear.tool.autotest.core.model.ScenarioResult;
import org.openbear.tool.autotest.core.model.StepResult;

public final class JUnitXmlResultReporter implements ResultReporter {
  @Override
  public ReportArtifact write(RunResult result, Path outputDirectory, ReportingOptions options)
      throws IOException {
    StringWriter output = new StringWriter();
    try {
      XMLStreamWriter xml = XMLOutputFactory.newFactory().createXMLStreamWriter(output);
      xml.writeStartDocument("UTF-8", "1.0");
      xml.writeStartElement("testsuite");
      xml.writeAttribute("name", value(result.getProject()));
      xml.writeAttribute("tests", Integer.toString(result.getScenarios().size()));
      xml.writeAttribute("failures", Long.toString(failures(result)));
      xml.writeAttribute("time", seconds(result.getDurationMs()));
      for (ScenarioResult scenario : result.getScenarios()) writeScenario(xml, scenario);
      xml.writeEndElement();
      xml.writeEndDocument();
      xml.close();
    } catch (XMLStreamException e) {
      throw new IOException("Failed to render JUnit XML", e);
    }
    Path path = outputDirectory.resolve("junit.xml");
    Files.writeString(path, output.toString(), StandardCharsets.UTF_8);
    return new ReportArtifact("junit", path);
  }

  private static void writeScenario(XMLStreamWriter xml, ScenarioResult scenario)
      throws XMLStreamException {
    xml.writeStartElement("testcase");
    xml.writeAttribute("classname", "autotest");
    xml.writeAttribute("name", value(scenario.getId()));
    xml.writeAttribute("time", seconds(scenario.getDurationMs()));
    if (scenario.getStatus().isFailure()) {
      String summary = failureSummary(scenario);
      xml.writeStartElement("failure");
      xml.writeAttribute("message", summary);
      xml.writeCharacters(summary);
      xml.writeEndElement();
    }
    xml.writeEndElement();
  }

  private static long failures(RunResult result) {
    return result.getScenarios().stream()
        .filter(scenario -> scenario.getStatus().isFailure())
        .count();
  }

  private static String failureSummary(ScenarioResult scenario) {
    List<String> failures = new ArrayList<>();
    for (StepResult step : all(scenario))
      if (step.getStatus().isFailure()) failures.add(step.getId() + ": " + step.getError());
    if (scenario.getError() != null) failures.add(scenario.getError());
    return String.join("; ", failures);
  }

  private static List<StepResult> all(ScenarioResult scenario) {
    List<StepResult> steps = new ArrayList<>();
    steps.addAll(scenario.getSetup());
    steps.addAll(scenario.getSteps());
    steps.addAll(scenario.getCleanup());
    return steps;
  }

  private static String seconds(long milliseconds) {
    return String.format(java.util.Locale.ROOT, "%.3f", milliseconds / 1000.0);
  }

  private static String value(String value) {
    return value == null ? "" : value;
  }
}
