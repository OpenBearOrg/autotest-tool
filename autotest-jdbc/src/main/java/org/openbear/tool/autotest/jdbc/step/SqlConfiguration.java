package org.openbear.tool.autotest.jdbc.step;

import java.util.Map;
import org.openbear.tool.autotest.spi.step.StepConfiguration;
import org.openbear.tool.autotest.spi.step.StepIdentity;

public record SqlConfiguration(
    String id,
    String name,
    String description,
    boolean continueOnFailure,
    String connection,
    String queryFile,
    String query,
    Map<String, Object> parameters,
    Map<String, Object> expect,
    Map<String, Object> capture)
    implements StepConfiguration {
  @Override
  public StepIdentity identity() {
    return new StepIdentity(id, name, description, continueOnFailure);
  }
}
