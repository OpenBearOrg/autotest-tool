package org.openbear.tool.autotest.core.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.openbear.tool.autotest.core.domain.ScenarioId;

class DefaultIdGeneratorTest {
  @Test
  void generatesDeterministicIdsWithAnInjectedClock() {
    Clock clock = Clock.fixed(Instant.parse("2026-08-22T10:00:00Z"), ZoneOffset.UTC);
    DefaultIdGenerator generator = new DefaultIdGenerator(clock);

    assertEquals("RUN-20260822-100000", generator.nextRunId().value());
    assertEquals(
        "TEST-20260822-100000-0001",
        generator.nextExecutionId(new ScenarioId("scenario-1")).value());
    assertEquals(
        "TEST-20260822-100000-0002",
        generator.nextExecutionId(new ScenarioId("scenario-1")).value());
  }
}
