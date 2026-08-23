package org.openbear.tool.autotest.core.model;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ComparisonResult {
  private String baselineRun;
  private String candidateRun;
  private String baselineEnvironment;
  private String candidateEnvironment;
  private boolean regression;
  private List<Entry> scenarios = new ArrayList<>();

  public void setScenarios(List<Entry> value) {
    scenarios = value == null ? new ArrayList<>() : value;
  }

  @Getter
  @Setter
  public static class Entry {
    private String id;
    private String baselineStatus;
    private String candidateStatus;
    private String classification;
    private boolean checksumChanged;
    private long baselineDurationMs;
    private long candidateDurationMs;
    private List<String> changedResources = new ArrayList<>();
    private List<String> stepChanges = new ArrayList<>();

    public void setChangedResources(List<String> value) {
      changedResources = value == null ? new ArrayList<>() : value;
    }

    public void setStepChanges(List<String> value) {
      stepChanges = value == null ? new ArrayList<>() : value;
    }
  }
}
