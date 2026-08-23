package org.openbear.tool.autotest.core.model;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SqlStepDefinition extends StepDefinition {
  private String connection;
  private String queryFile;
  private String query;
  private Map<String, Object> parameters = new LinkedHashMap<>();
  private ExpectedValues expect = new ExpectedValues();
  private Map<String, CaptureDefinition> capture = new LinkedHashMap<>();

  @Override
  public String type() {
    return "sql";
  }

  public void setParameters(Map<String, Object> parameters) {
    this.parameters = parameters == null ? new LinkedHashMap<>() : parameters;
  }

  public void setExpect(ExpectedValues expect) {
    this.expect = expect == null ? new ExpectedValues() : expect;
  }

  public void setCapture(Map<String, CaptureDefinition> capture) {
    this.capture = capture == null ? new LinkedHashMap<>() : capture;
  }
}
