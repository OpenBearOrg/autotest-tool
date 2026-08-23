package org.openbear.tool.autotest.reporting;

import org.openbear.tool.autotest.core.config.ReportingConfig;

public record ReportingOptions(ReportingConfig config) {
  public ReportingOptions {
    config = config == null ? new ReportingConfig() : config;
  }
}
