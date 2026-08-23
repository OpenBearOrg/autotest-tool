package org.openbear.tool.autotest.core.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.openbear.tool.autotest.core.model.ComparisonResult;
import org.openbear.tool.autotest.core.model.ResultStatus;
import org.openbear.tool.autotest.core.model.RunResult;
import org.openbear.tool.autotest.core.model.ScenarioResult;
import org.openbear.tool.autotest.core.model.StepResult;

public final class ComparisonCalculator {
  private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

  public ComparisonResult compare(Path baseline, Path candidate) throws IOException {
    return compare(
        mapper.readValue(baseline.toFile(), RunResult.class),
        mapper.readValue(candidate.toFile(), RunResult.class));
  }

  public ComparisonResult compare(RunResult baseline, RunResult candidate) {
    ComparisonResult result = new ComparisonResult();
    result.setBaselineRun(baseline.getRunId());
    result.setCandidateRun(candidate.getRunId());
    result.setBaselineEnvironment(baseline.getEnvironment());
    result.setCandidateEnvironment(candidate.getEnvironment());
    Map<String, ScenarioResult> baselineScenarios = index(baseline);
    Map<String, ScenarioResult> candidateScenarios = index(candidate);
    LinkedHashSet<String> ids = new LinkedHashSet<>(baselineScenarios.keySet());
    ids.addAll(candidateScenarios.keySet());
    boolean regression = false;
    for (String id : ids) {
      ScenarioResult before = baselineScenarios.get(id);
      ScenarioResult after = candidateScenarios.get(id);
      ComparisonResult.Entry entry = new ComparisonResult.Entry();
      entry.setId(id);
      entry.setBaselineStatus(before == null ? "MISSING" : before.getStatus().name());
      entry.setCandidateStatus(after == null ? "MISSING" : after.getStatus().name());
      entry.setBaselineDurationMs(before == null ? 0 : before.getDurationMs());
      entry.setCandidateDurationMs(after == null ? 0 : after.getDurationMs());
      List<String> changedResources = resourceChanges(before, after);
      entry.setChangedResources(changedResources);
      entry.setChecksumChanged(
          before != null
              && after != null
              && (!Objects.equals(before.getChecksum(), after.getChecksum())
                  || !changedResources.isEmpty()));
      String classification = classify(before, after);
      entry.setClassification(classification);
      entry.setStepChanges(stepChanges(before, after));
      if ("REGRESSION".equals(classification)) regression = true;
      result.getScenarios().add(entry);
    }
    result.setRegression(regression);
    return result;
  }

  public String console(ComparisonResult result) {
    StringBuilder out =
        new StringBuilder("Upgrade Comparison\n")
            .append(result.getBaselineEnvironment())
            .append(" -> ")
            .append(result.getCandidateEnvironment())
            .append("\n\n");
    for (ComparisonResult.Entry entry : result.getScenarios())
      out.append(
          String.format(
              "%-36s %-10s %-10s %-12s%s%n",
              entry.getId(),
              entry.getBaselineStatus(),
              entry.getCandidateStatus(),
              entry.getClassification(),
              entry.isChecksumChanged() ? " [CHECKSUM CHANGED]" : ""));
    return out.append("\nRegression: ").append(result.isRegression() ? "YES" : "NO").toString();
  }

  private static String classify(ScenarioResult baseline, ScenarioResult candidate) {
    if (baseline == null) return "NEW";
    if (candidate == null) return "MISSING";
    if (baseline.getStatus() == ResultStatus.PASS && candidate.getStatus().isFailure())
      return "REGRESSION";
    if (baseline.getStatus().isFailure() && candidate.getStatus() == ResultStatus.PASS)
      return "FIXED";
    if (baseline.getStatus() == candidate.getStatus()) return "UNCHANGED";
    return "CHANGED";
  }

  private static Map<String, ScenarioResult> index(RunResult run) {
    Map<String, ScenarioResult> scenarios = new LinkedHashMap<>();
    for (ScenarioResult scenario : run.getScenarios()) scenarios.put(scenario.getId(), scenario);
    return scenarios;
  }

  private static List<String> resourceChanges(ScenarioResult baseline, ScenarioResult candidate) {
    if (baseline == null || candidate == null) return List.of();
    LinkedHashSet<String> keys = new LinkedHashSet<>(baseline.getResourceChecksums().keySet());
    keys.addAll(candidate.getResourceChecksums().keySet());
    List<String> changes = new ArrayList<>();
    for (String key : keys)
      if (!Objects.equals(
          baseline.getResourceChecksums().get(key), candidate.getResourceChecksums().get(key)))
        changes.add(key);
    return changes;
  }

  private static List<String> stepChanges(ScenarioResult baseline, ScenarioResult candidate) {
    if (baseline == null || candidate == null) return List.of();
    Map<String, StepResult> before = steps(baseline);
    Map<String, StepResult> after = steps(candidate);
    List<String> changes = new ArrayList<>();
    for (String id : before.keySet()) {
      StepResult left = before.get(id), right = after.get(id);
      if (right != null && left.getStatus() != right.getStatus())
        changes.add(id + ": " + left.getStatus() + " -> " + right.getStatus());
    }
    return changes;
  }

  private static Map<String, StepResult> steps(ScenarioResult scenario) {
    Map<String, StepResult> steps = new LinkedHashMap<>();
    List<StepResult> all = new ArrayList<>();
    all.addAll(scenario.getSetup());
    all.addAll(scenario.getSteps());
    all.addAll(scenario.getCleanup());
    for (StepResult step : all) steps.put(step.getId(), step);
    return steps;
  }
}
