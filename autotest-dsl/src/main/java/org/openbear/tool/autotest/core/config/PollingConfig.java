package org.openbear.tool.autotest.core.config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PollingConfig {
  private String timeout;
  private String interval;

  public PollingConfig() {}

  public PollingConfig(String timeout, String interval) {
    this.timeout = timeout;
    this.interval = interval;
  }

  public PollingConfig mergedOver(PollingConfig defaults) {
    PollingConfig out = new PollingConfig();
    out.timeout = timeout != null ? timeout : defaults == null ? null : defaults.timeout;
    out.interval = interval != null ? interval : defaults == null ? null : defaults.interval;
    return out;
  }
}
