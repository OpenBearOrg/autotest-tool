package org.openbear.tool.autotest.core.domain;

public record ExecutionSettings(int parallelism, boolean failFast, boolean runDoctorChecks) {
  public ExecutionSettings {
    if (parallelism < 1) throw new IllegalArgumentException("parallelism must be >= 1");
  }
}
