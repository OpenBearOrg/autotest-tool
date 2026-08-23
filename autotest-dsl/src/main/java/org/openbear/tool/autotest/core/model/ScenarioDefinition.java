package org.openbear.tool.autotest.core.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScenarioDefinition {
  private String dslVersion = "1.0";
  private String id;
  private String name;
  private List<String> tags = new ArrayList<>();
  private List<String> requiredVariables = new ArrayList<>();
  private Map<String, Object> variables = new LinkedHashMap<>();
  private List<StepDefinition> setup = new ArrayList<>();
  private List<StepDefinition> steps = new ArrayList<>();
  private List<StepDefinition> cleanup = new ArrayList<>();
  private ScenarioExecution execution = new ScenarioExecution();

  public void setTags(List<String> tags) {
    this.tags = tags == null ? new ArrayList<>() : tags;
  }

  public void setRequiredVariables(List<String> requiredVariables) {
    this.requiredVariables = requiredVariables == null ? new ArrayList<>() : requiredVariables;
  }

  public void setVariables(Map<String, Object> variables) {
    this.variables = variables == null ? new LinkedHashMap<>() : variables;
  }

  public void setSetup(List<StepDefinition> setup) {
    this.setup = setup == null ? new ArrayList<>() : setup;
  }

  public void setSteps(List<StepDefinition> steps) {
    this.steps = steps == null ? new ArrayList<>() : steps;
  }

  public void setCleanup(List<StepDefinition> cleanup) {
    this.cleanup = cleanup == null ? new ArrayList<>() : cleanup;
  }

  public void setExecution(ScenarioExecution execution) {
    this.execution = execution == null ? new ScenarioExecution() : execution;
  }
}
