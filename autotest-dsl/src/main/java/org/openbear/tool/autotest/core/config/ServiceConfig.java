package org.openbear.tool.autotest.core.config;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ServiceConfig {
  private String baseUrl;
  private String connectTimeout = "10s";
  private String requestTimeout = "60s";
  private String healthPath = "/";
  private Integer safeRetryAttempts = 2;
  private Map<String, String> defaultHeaders = new LinkedHashMap<>();

  public void setDefaultHeaders(Map<String, String> defaultHeaders) {
    this.defaultHeaders = defaultHeaders == null ? new LinkedHashMap<>() : defaultHeaders;
  }
}
