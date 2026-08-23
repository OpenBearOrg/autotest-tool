package org.openbear.tool.autotest.reporting;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.openbear.tool.autotest.core.config.ReportingConfig;
import org.openbear.tool.autotest.core.model.PollObservation;
import org.openbear.tool.autotest.core.model.ResultStatus;
import org.openbear.tool.autotest.core.model.RunResult;
import org.openbear.tool.autotest.core.model.ScenarioResult;
import org.openbear.tool.autotest.core.model.StepResult;

public final class DefaultRunReportWriter {
  private final ObjectMapper mapper =
      new ObjectMapper()
          .findAndRegisterModules()
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

  public Path write(RunResult run, Path directory, ReportingConfig config) throws IOException {
    new ReportService().writeAll(run, directory, new ReportingOptions(config));
    return directory;
  }

  Path writeHtmlBundle(RunResult run, Path directory, ReportingConfig config) throws IOException {
    Files.createDirectories(directory);
    Files.createDirectories(directory.resolve("scenarios"));
    EvidenceSanitizer sanitizer = new EvidenceSanitizer(mapper, config);
    Files.writeString(directory.resolve("index.html"), index(run), StandardCharsets.UTF_8);
    for (ScenarioResult scenario : run.getScenarios())
      Files.writeString(
          directory.resolve("scenarios").resolve(safe(scenario.getId()) + ".html"),
          scenario(scenario, sanitizer),
          StandardCharsets.UTF_8);
    return directory;
  }

  private static String safe(String value) {
    return value.replaceAll("[^A-Za-z0-9._-]", "_");
  }

  private String index(RunResult run) {
    long pass = run.getScenarios().stream().filter(s -> s.getStatus() == ResultStatus.PASS).count();
    long failed = run.getScenarios().stream().filter(s -> s.getStatus().isFailure()).count();
    long skipped =
        run.getScenarios().stream().filter(s -> s.getStatus() == ResultStatus.SKIPPED).count();
    double rate = run.getScenarios().isEmpty() ? 0 : pass * 100.0 / run.getScenarios().size();
    StringBuilder body = new StringBuilder();
    body.append("<h1>")
        .append(Html.esc(run.getProject()))
        .append(" <span class=\"status ")
        .append(run.getStatus())
        .append("\">")
        .append(run.getStatus())
        .append("</span></h1><div class=card><b>Run:</b> ")
        .append(Html.esc(run.getRunId()))
        .append(" &nbsp; <b>Environment:</b> ")
        .append(Html.esc(run.getEnvironment()))
        .append("<br><span class=\"muted\">Duration: ")
        .append(run.getDurationMs())
        .append(
            " ms</span></div><div class=grid><div class=kpi><span class=muted>Total</span><strong>")
        .append(run.getScenarios().size())
        .append("</strong></div><div class=kpi><span class=muted>Passed</span><strong class=PASS>")
        .append(pass)
        .append("</strong></div><div class=kpi><span class=muted>Failed</span><strong class=FAIL>")
        .append(failed)
        .append(
            "</strong></div><div class=kpi><span class=muted>Skipped</span><strong class=SKIPPED>")
        .append(skipped)
        .append("</strong></div></div><div class=card><b>Pass rate: ")
        .append(String.format("%.1f", rate))
        .append("%</b><div class=progress aria-label=\"Pass rate\"><span style=\"width:")
        .append(String.format("%.1f", rate))
        .append(
            "%\"></span></div></div><div class=toolbar><label for=search>Search scenarios</label><input id=search data-report-search placeholder=\"ID or name\"><label for=filter>Status</label><select id=filter data-report-filter><option value=ALL>All</option><option>PASS</option><option>FAIL</option><option>ERROR</option><option>SKIPPED</option></select></div><table><tr><th>Scenario</th><th>Status</th><th>Duration</th><th>Execution</th></tr>");
    for (ScenarioResult scenario : run.getScenarios())
      body.append("<tr data-report-row data-status=\"")
          .append(scenario.getStatus())
          .append("\"><td><a href=\"scenarios/")
          .append(safe(scenario.getId()))
          .append(".html\">")
          .append(Html.esc(scenario.getId()))
          .append(" — ")
          .append(Html.esc(scenario.getName()))
          .append("</a></td><td class=\"status ")
          .append(scenario.getStatus())
          .append("\">")
          .append(scenario.getStatus())
          .append("</td><td>")
          .append(scenario.getDurationMs())
          .append(" ms</td><td>")
          .append(Html.esc(scenario.getExecutionId()))
          .append("</td></tr>");
    body.append("</table>");
    return Html.page("Autotest Report", body.toString());
  }

  private String scenario(ScenarioResult scenario, EvidenceSanitizer sanitizer) {
    StringBuilder body = new StringBuilder();
    body.append("<a href=\"../index.html\">← Run</a><h1>")
        .append(Html.esc(scenario.getId()))
        .append(" — ")
        .append(Html.esc(scenario.getName()))
        .append("</h1><div class=card><b>Status:</b> <span class=\"status ")
        .append(scenario.getStatus())
        .append("\">")
        .append(scenario.getStatus())
        .append("</span> &nbsp; <b>Execution:</b> ")
        .append(Html.esc(scenario.getExecutionId()))
        .append(" &nbsp; <b>Duration:</b> ")
        .append(scenario.getDurationMs())
        .append(" ms<br><b>Scenario SHA-256:</b> <code>")
        .append(Html.esc(scenario.getChecksum()))
        .append("</code></div>");
    appendPhase(body, "Setup", scenario.getSetup(), sanitizer);
    appendPhase(body, "Steps", scenario.getSteps(), sanitizer);
    appendPhase(body, "Cleanup", scenario.getCleanup(), sanitizer);
    return Html.page(scenario.getId(), body.toString());
  }

  private void appendPhase(
      StringBuilder body, String title, List<StepResult> steps, EvidenceSanitizer sanitizer) {
    if (steps.isEmpty()) return;
    body.append("<h2>").append(title).append("</h2><div class=timeline>");
    for (StepResult step : steps) {
      body.append("<div class=\"card step ")
          .append(step.getStatus())
          .append("\"><h3>")
          .append(Html.esc(step.getId()))
          .append(" — ")
          .append(Html.esc(step.getName()))
          .append(" <span class=\"status ")
          .append(step.getStatus())
          .append("\">")
          .append(step.getStatus())
          .append("</span></h3><div class=muted>")
          .append(Html.esc(step.getType()))
          .append(" · ")
          .append(step.getDurationMs())
          .append(" ms</div>");
      if (step.getDescription() != null && !step.getDescription().isBlank())
        body.append("<p>").append(Html.esc(step.getDescription())).append("</p>");
      if (step.getError() != null)
        body.append("<p><b>Error:</b> ").append(Html.esc(step.getError())).append("</p>");
      try {
        appendEvidencePanels(body, step, sanitizer);
        body.append("<details><summary>Evidence</summary><pre>")
            .append(
                Html.esc(
                    mapper
                        .writerWithDefaultPrettyPrinter()
                        .writeValueAsString(sanitizer.sanitize(step.getEvidence()))))
            .append("</pre></details>");
        if (!step.getObservations().isEmpty()) {
          body.append("<details><summary>State transitions (")
              .append(step.getObservations().size())
              .append(")</summary><ol>");
          for (PollObservation observation : step.getObservations())
            body.append("<li><b>")
                .append(observation.getElapsedMs())
                .append(" ms</b> — ")
                .append(
                    Html.esc(mapper.writeValueAsString(sanitizer.sanitize(observation.getValue()))))
                .append("</li>");
          body.append("</ol><details><summary>Raw polling observations</summary><pre>")
              .append(
                  Html.esc(
                      mapper
                          .writerWithDefaultPrettyPrinter()
                          .writeValueAsString(sanitizer.sanitize(step.getObservations()))))
              .append("</pre></details></details>");
        }
      } catch (Exception ignored) {
      }
      body.append("</div>");
    }
    body.append("</div>");
  }

  private void appendEvidencePanels(
      StringBuilder body, StepResult step, EvidenceSanitizer sanitizer) throws IOException {
    if (step.getEvidence().containsKey("request"))
      panel(body, "Request", step.getEvidence().get("request"), sanitizer);
    if (step.getEvidence().containsKey("response"))
      panel(body, "Response", step.getEvidence().get("response"), sanitizer);
    if (step.getEvidence().containsKey("result"))
      panel(body, "Query result", step.getEvidence().get("result"), sanitizer);
    if (step.getEvidence().containsKey("message"))
      panel(body, "Message", step.getEvidence().get("message"), sanitizer);
  }

  private void panel(StringBuilder body, String title, Object value, EvidenceSanitizer sanitizer)
      throws IOException {
    body.append("<details><summary>")
        .append(title)
        .append("</summary><pre>")
        .append(
            Html.esc(
                mapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(sanitizer.sanitize(value))))
        .append("</pre></details>");
  }
}
