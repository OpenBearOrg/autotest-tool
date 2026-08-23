package org.openbear.tool.autotest.core.config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MessagingConfig {
  private String type = "activemq";
  private String brokerUrl;
  private SecretRef username;
  private SecretRef password;
  private String connectTimeout = "10s";
}
