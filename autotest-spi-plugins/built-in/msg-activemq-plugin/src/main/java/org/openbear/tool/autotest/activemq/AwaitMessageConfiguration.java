package org.openbear.tool.autotest.activemq;

import java.util.Map;
import org.openbear.tool.autotest.spi.step.StepConfiguration;
import org.openbear.tool.autotest.spi.step.StepIdentity;

/** YAML-facing configuration owned by the ActiveMQ plugin. */
public record AwaitMessageConfiguration(
    String id,
    String name,
    String description,
    boolean continueOnFailure,
    String connection,
    String destination,
    String observationMode,
    String selector,
    Map<String, Object> polling,
    Map<String, Object> match,
    Map<String, Object> expect,
    Map<String, Object> capture)
    implements StepConfiguration {
  @Override
  public StepIdentity identity() {
    return new StepIdentity(id, name, description, continueOnFailure);
  }
}
