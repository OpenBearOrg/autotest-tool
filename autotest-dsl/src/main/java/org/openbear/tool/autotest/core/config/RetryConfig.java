package org.openbear.tool.autotest.core.config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RetryConfig {
  private Boolean enabled;
  private Integer maxAttempts;
  private String delay;
}
