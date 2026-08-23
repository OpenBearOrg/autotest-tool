package org.openbear.tool.autotest.core.domain;

public record ScenarioId(String value) {
  public ScenarioId {
    value = IdValue.requireNonBlank(value, "scenarioId");
  }
}
