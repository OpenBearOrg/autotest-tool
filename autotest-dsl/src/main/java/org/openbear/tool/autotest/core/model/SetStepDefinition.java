package org.openbear.tool.autotest.core.model;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SetStepDefinition extends StepDefinition {
  private Map<String, Object> values = new LinkedHashMap<>();

  @Override
  public String type() {
    return "set";
  }

  public void setValues(Map<String, Object> values) {
    this.values = values == null ? new LinkedHashMap<>() : values;
  }
}
