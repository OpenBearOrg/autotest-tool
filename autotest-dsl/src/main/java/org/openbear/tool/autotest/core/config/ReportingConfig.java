package org.openbear.tool.autotest.core.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReportingConfig {
  private String directory = "reports";
  private String payloadMaxSize = "1MB";
  private List<String> redactedFields =
      new ArrayList<>(
          Arrays.asList(
              "authorization", "password", "token", "client_secret", "secret", "card_number"));

  public void setRedactedFields(List<String> redactedFields) {
    this.redactedFields = redactedFields == null ? new ArrayList<>() : redactedFields;
  }
}
