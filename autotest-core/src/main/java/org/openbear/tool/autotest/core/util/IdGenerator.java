package org.openbear.tool.autotest.core.util;

import java.time.Clock;
import org.openbear.tool.autotest.core.engine.DefaultIdGenerator;

public final class IdGenerator {
  private IdGenerator() {}

  /**
   * @deprecated Use {@link DefaultIdGenerator} through dependency injection.
   */
  @Deprecated(forRemoval = false)
  public static String runId() {
    return new DefaultIdGenerator(Clock.systemUTC()).nextRunId().value();
  }

  /**
   * @deprecated Use {@link DefaultIdGenerator} through dependency injection.
   */
  @Deprecated(forRemoval = false)
  public static String executionId() {
    return new DefaultIdGenerator(Clock.systemUTC())
        .nextExecutionId(new org.openbear.tool.autotest.core.domain.ScenarioId("legacy"))
        .value();
  }
}
