package org.openbear.tool.autotest.core.model;

import lombok.Getter;
import lombok.Setter;
import org.openbear.tool.autotest.core.config.PollingConfig;

@Getter
@Setter
public class AwaitSqlStepDefinition extends SqlStepDefinition {
  private PollingConfig polling = new PollingConfig();

  @Override
  public String type() {
    return "awaitSql";
  }

  public void setPolling(PollingConfig polling) {
    this.polling = polling == null ? new PollingConfig() : polling;
  }
}
