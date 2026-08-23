package org.openbear.tool.autotest.http;

import java.util.Map;
import org.openbear.tool.autotest.spi.step.StepConfiguration;
import org.openbear.tool.autotest.spi.step.StepIdentity;

/** YAML-facing configuration owned by the HTTP plugin. */
public record HttpConfiguration(
    String id,
    String name,
    String description,
    boolean continueOnFailure,
    String service,
    Map<String, Object> request,
    Map<String, Object> expect,
    Map<String, Object> capture)
    implements StepConfiguration {
  @Override
  public StepIdentity identity() {
    return new StepIdentity(id, name, description, continueOnFailure);
  }
}
