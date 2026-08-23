package org.openbear.tool.autotest.core.model;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HttpStepDefinition extends StepDefinition {
  private String service;
  private HttpRequestDefinition request = new HttpRequestDefinition();
  private HttpExpect expect = new HttpExpect();
  private Map<String, CaptureDefinition> capture = new LinkedHashMap<>();

  @Override
  public String type() {
    return "http";
  }

  public void setRequest(HttpRequestDefinition request) {
    this.request = request == null ? new HttpRequestDefinition() : request;
  }

  public void setExpect(HttpExpect expect) {
    this.expect = expect == null ? new HttpExpect() : expect;
  }

  public void setCapture(Map<String, CaptureDefinition> capture) {
    this.capture = capture == null ? new LinkedHashMap<>() : capture;
  }
}
