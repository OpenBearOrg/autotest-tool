package org.openbear.tool.autotest.spi.step;

public interface ExecutableStep {
  StepIdentity identity();

  String type();
}
