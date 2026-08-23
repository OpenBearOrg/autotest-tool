package org.openbear.tool.autotest.core.config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProjectDefaults {
  private String environment;
  private PollingConfig polling = new PollingConfig("2m", "3s");
}
