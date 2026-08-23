package org.openbear.tool.autotest.core.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RunResult {
  private String toolVersion;
  private String runId;
  private String label;
  private String project;
  private String environment;
  private String gitCommit;
  private Instant startedAt;
  private Instant endedAt;
  private long durationMs;
  private ResultStatus status;
  private Map<String, Object> metadata = new LinkedHashMap<>();
  private List<ScenarioResult> scenarios = new ArrayList<>();

  public void setMetadata(Map<String, Object> metadata) {
    this.metadata = metadata == null ? new LinkedHashMap<>() : metadata;
  }

  public void setScenarios(List<ScenarioResult> scenarios) {
    this.scenarios = scenarios == null ? new ArrayList<>() : scenarios;
  }
}
