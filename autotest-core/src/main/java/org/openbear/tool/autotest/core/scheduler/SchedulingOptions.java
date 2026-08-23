package org.openbear.tool.autotest.core.scheduler;

public record SchedulingOptions(int parallelism, boolean failFast) {
  public SchedulingOptions {
    if (parallelism < 1) throw new IllegalArgumentException("parallelism must be >= 1");
  }
}
