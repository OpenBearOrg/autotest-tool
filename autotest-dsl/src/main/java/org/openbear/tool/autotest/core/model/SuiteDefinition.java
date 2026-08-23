package org.openbear.tool.autotest.core.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SuiteDefinition {
  private String suiteVersion = "1.0";
  private String id;
  private String name;
  private Map<String, Object> variables = new LinkedHashMap<>();
  private List<String> scenarios = new ArrayList<>();
  private List<String> includeTags = new ArrayList<>();
  private List<String> excludeTags = new ArrayList<>();

  public void setVariables(Map<String, Object> variables) {
    this.variables = variables == null ? new LinkedHashMap<>() : variables;
  }

  public void setScenarios(List<String> scenarios) {
    this.scenarios = scenarios == null ? new ArrayList<>() : scenarios;
  }

  public void setIncludeTags(List<String> includeTags) {
    this.includeTags = includeTags == null ? new ArrayList<>() : includeTags;
  }

  public void setExcludeTags(List<String> excludeTags) {
    this.excludeTags = excludeTags == null ? new ArrayList<>() : excludeTags;
  }
}
