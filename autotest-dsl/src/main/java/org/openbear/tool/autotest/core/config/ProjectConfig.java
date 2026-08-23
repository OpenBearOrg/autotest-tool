package org.openbear.tool.autotest.core.config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProjectConfig {
  private String projectVersion = "1.0";
  private String name = "Autotest Workspace";
  private ProjectDefaults defaults = new ProjectDefaults();
  private ReportingConfig reporting = new ReportingConfig();
  private ProjectExecutionConfig execution = new ProjectExecutionConfig();

  public void setDefaults(ProjectDefaults defaults) {
    this.defaults = defaults == null ? new ProjectDefaults() : defaults;
  }

  public void setReporting(ReportingConfig reporting) {
    this.reporting = reporting == null ? new ReportingConfig() : reporting;
  }

  public void setExecution(ProjectExecutionConfig execution) {
    this.execution = execution == null ? new ProjectExecutionConfig() : execution;
  }
}
