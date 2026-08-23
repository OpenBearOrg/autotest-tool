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
public class ScenarioResult {
  private String id;
  private String name;
  private ResultStatus status;
  private String executionId;
  private Instant startedAt;
  private Instant endedAt;
  private long durationMs;
  private String checksum;
  private Map<String, String> resourceChecksums = new LinkedHashMap<>();
  private Map<String, Object> finalVariables = new LinkedHashMap<>();
  private List<StepResult> setup = new ArrayList<>();
  private List<StepResult> steps = new ArrayList<>();
  private List<StepResult> cleanup = new ArrayList<>();
  private String error;

  public void setResourceChecksums(Map<String, String> resourceChecksums) {
    this.resourceChecksums = resourceChecksums == null ? new LinkedHashMap<>() : resourceChecksums;
  }

  public void setFinalVariables(Map<String, Object> finalVariables) {
    this.finalVariables = finalVariables == null ? new LinkedHashMap<>() : finalVariables;
  }

  public void setSetup(List<StepResult> setup) {
    this.setup = setup == null ? new ArrayList<>() : setup;
  }

  public void setSteps(List<StepResult> steps) {
    this.steps = steps == null ? new ArrayList<>() : steps;
  }

  public void setCleanup(List<StepResult> cleanup) {
    this.cleanup = cleanup == null ? new ArrayList<>() : cleanup;
  }
}
