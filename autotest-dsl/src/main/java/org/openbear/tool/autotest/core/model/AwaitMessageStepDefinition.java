package org.openbear.tool.autotest.core.model;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.openbear.tool.autotest.core.config.PollingConfig;

@Getter
@Setter
public class AwaitMessageStepDefinition extends StepDefinition {
  private String connection;
  private String destination;
  private String observationMode = "dedicated";
  private String selector;
  private PollingConfig polling = new PollingConfig();
  private MessageMatchDefinition match = new MessageMatchDefinition();
  private ExpectedValues expect = new ExpectedValues();
  private Map<String, CaptureDefinition> capture = new LinkedHashMap<>();

  @Override
  public String type() {
    return "awaitMessage";
  }

  public void setPolling(PollingConfig polling) {
    this.polling = polling == null ? new PollingConfig() : polling;
  }

  public void setMatch(MessageMatchDefinition match) {
    this.match = match == null ? new MessageMatchDefinition() : match;
  }

  public void setExpect(ExpectedValues expect) {
    this.expect = expect == null ? new ExpectedValues() : expect;
  }

  public void setCapture(Map<String, CaptureDefinition> capture) {
    this.capture = capture == null ? new LinkedHashMap<>() : capture;
  }
}
