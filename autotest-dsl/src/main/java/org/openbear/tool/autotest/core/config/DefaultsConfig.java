package org.openbear.tool.autotest.core.config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DefaultsConfig {
  private PollingConfig polling = new PollingConfig("2m", "3s");

  public void setPolling(PollingConfig polling) {
    this.polling = polling;
  }
}
