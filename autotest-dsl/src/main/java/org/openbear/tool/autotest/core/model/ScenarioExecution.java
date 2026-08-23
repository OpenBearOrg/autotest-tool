package org.openbear.tool.autotest.core.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScenarioExecution {
  private String isolation = "sequential";

  public boolean isParallelSafe() {
    return "parallel-safe".equalsIgnoreCase(isolation);
  }
}
