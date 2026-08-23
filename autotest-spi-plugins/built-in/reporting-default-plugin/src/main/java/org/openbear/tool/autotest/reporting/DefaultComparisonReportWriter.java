package org.openbear.tool.autotest.reporting;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.openbear.tool.autotest.core.model.ComparisonResult;

public final class DefaultComparisonReportWriter {
  private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

  public Path write(ComparisonResult result, Path directory) throws IOException {
    Files.createDirectories(directory);
    mapper
        .writerWithDefaultPrettyPrinter()
        .writeValue(directory.resolve("comparison.json").toFile(), result);
    Files.writeString(directory.resolve("comparison.html"), html(result), StandardCharsets.UTF_8);
    return directory;
  }

  private String html(ComparisonResult result) {
    long regressions =
        result.getScenarios().stream()
            .filter(e -> "REGRESSION".equals(e.getClassification()))
            .count();
    long fixed =
        result.getScenarios().stream().filter(e -> "FIXED".equals(e.getClassification())).count();
    long added =
        result.getScenarios().stream().filter(e -> "NEW".equals(e.getClassification())).count();
    long missing =
        result.getScenarios().stream().filter(e -> "MISSING".equals(e.getClassification())).count();
    StringBuilder body =
        new StringBuilder("<h1>Upgrade Comparison <span class=\"badge ")
            .append(result.isRegression() ? "FAIL\">REGRESSION" : "PASS\">NO REGRESSION")
            .append("</span></h1><div class=card><b>Baseline:</b> ")
            .append(Html.esc(result.getBaselineEnvironment()))
            .append(" / ")
            .append(Html.esc(result.getBaselineRun()))
            .append("<br><b>Candidate:</b> ")
            .append(Html.esc(result.getCandidateEnvironment()))
            .append(" / ")
            .append(Html.esc(result.getCandidateRun()))
            .append("<br><b>Regression:</b> ")
            .append(result.isRegression() ? "YES" : "NO")
            .append(
                "</div><div class=grid><div class=kpi><span class=muted>Regressions</span><strong class=FAIL>")
            .append(regressions)
            .append(
                "</strong></div><div class=kpi><span class=muted>Fixes</span><strong class=PASS>")
            .append(fixed)
            .append("</strong></div><div class=kpi><span class=muted>New</span><strong>")
            .append(added)
            .append("</strong></div><div class=kpi><span class=muted>Missing</span><strong>")
            .append(missing)
            .append(
                "</strong></div></div><div class=toolbar><label for=filter>Show</label><select id=filter data-report-filter><option value=REGRESSION>Regressions only</option><option value=ALL>All classifications</option><option>FIXED</option><option>NEW</option><option>MISSING</option><option>UNCHANGED</option></select></div><table><tr><th>Scenario</th><th>Baseline</th><th>Candidate</th><th>Classification</th><th>Duration Δ</th><th>Notes</th></tr>");
    for (ComparisonResult.Entry entry : result.getScenarios())
      body.append("<tr data-report-row data-classification=\"")
          .append(entry.getClassification())
          .append("\"><td>")
          .append(Html.esc(entry.getId()))
          .append("</td><td>")
          .append(entry.getBaselineStatus())
          .append("</td><td>")
          .append(entry.getCandidateStatus())
          .append("</td><td><span class=\"badge ")
          .append(entry.getClassification())
          .append("\">")
          .append(entry.getClassification())
          .append("</span></td><td class=\"")
          .append(
              entry.getCandidateDurationMs() > entry.getBaselineDurationMs()
                  ? "delta-up"
                  : "delta-down")
          .append("\"><b>")
          .append(entry.getCandidateDurationMs() - entry.getBaselineDurationMs())
          .append(" ms</b><br><span class=muted>Baseline ")
          .append(entry.getBaselineDurationMs())
          .append(" ms · Candidate ")
          .append(entry.getCandidateDurationMs())
          .append(" ms</span></td><td>")
          .append(entry.isChecksumChanged() ? "Scenario/resources changed. " : "")
          .append(
              entry.getChangedResources().isEmpty()
                  ? ""
                  : "Resources: " + Html.esc(String.join(", ", entry.getChangedResources())) + ". ")
          .append(Html.esc(String.join("; ", entry.getStepChanges())))
          .append("</td></tr>");
    return Html.page("Upgrade Comparison", body.append("</table>").toString());
  }
}
