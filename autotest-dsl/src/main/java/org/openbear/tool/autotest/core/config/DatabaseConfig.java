package org.openbear.tool.autotest.core.config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DatabaseConfig {
  private String driver = "oracle";
  private String jdbcUrl;
  private SecretRef username;
  private SecretRef password;
  private Integer maximumPoolSize = 5;
  private String connectionTimeout = "15s";
  private String validationTimeout = "5s";
}
