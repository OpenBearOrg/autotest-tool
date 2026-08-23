package org.openbear.tool.autotest.jdbc.step;

import java.util.Map;
import org.openbear.tool.autotest.spi.step.ExecutableStep;
import org.openbear.tool.autotest.spi.step.StepIdentity;

public record AwaitSqlExecutableStep(
    StepIdentity identity,
    String connection,
    String queryFile,
    String query,
    Map<String, Object> parameters,
    Map<String, Object> expect,
    Map<String, Object> capture,
    Map<String, Object> polling)
    implements ExecutableStep {
  @Override
  public String type() {
    return "awaitSql";
  }
}
