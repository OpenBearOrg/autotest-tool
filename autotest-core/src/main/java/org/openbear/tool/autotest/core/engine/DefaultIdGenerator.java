package org.openbear.tool.autotest.core.engine;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import org.openbear.tool.autotest.core.domain.ExecutionId;
import org.openbear.tool.autotest.core.domain.RunId;
import org.openbear.tool.autotest.core.domain.ScenarioId;

public final class DefaultIdGenerator implements IdGenerator {
  private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

  private final Clock clock;
  private final AtomicInteger sequence = new AtomicInteger();

  public DefaultIdGenerator(Clock clock) {
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  @Override
  public RunId nextRunId() {
    return new RunId("RUN-" + timestamp());
  }

  @Override
  public ExecutionId nextExecutionId(ScenarioId scenarioId) {
    Objects.requireNonNull(scenarioId, "scenarioId");
    int value = sequence.updateAndGet(current -> current >= 9999 ? 1 : current + 1);
    return new ExecutionId("TEST-" + timestamp() + "-" + String.format("%04d", value));
  }

  private String timestamp() {
    return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC).format(FORMAT);
  }
}
