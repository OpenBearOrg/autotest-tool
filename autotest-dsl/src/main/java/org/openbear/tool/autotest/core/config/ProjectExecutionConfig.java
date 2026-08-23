package org.openbear.tool.autotest.core.config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProjectExecutionConfig {
  private int parallelism = 1;
  private boolean failFast = false;
}
