package org.openbear.tool.autotest.core.event;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openbear.tool.autotest.core.domain.EnvironmentId;
import org.openbear.tool.autotest.core.domain.RunId;

class ExecutionEventDispatcherTest {
  @Test
  void sequencesEventsAndContinuesAfterListenerFailure() {
    List<ExecutionEventEnvelope> received = new ArrayList<>();
    ExecutionEventDispatcher dispatcher =
        new ExecutionEventDispatcher(
            Clock.systemUTC(),
            List.of(
                event -> {
                  throw new IllegalStateException("presentation failure");
                },
                received::add));

    assertDoesNotThrow(
        () ->
            dispatcher.publish(
                new RunStarted(new RunId("run-1"), "project", new EnvironmentId("local"))));
    assertDoesNotThrow(
        () ->
            dispatcher.publish(
                new RunFinished(
                    new RunId("run-1"),
                    org.openbear.tool.autotest.core.model.ResultStatus.PASS,
                    java.time.Duration.ZERO,
                    0,
                    null)));

    assertEquals(List.of(1L, 2L), received.stream().map(ExecutionEventEnvelope::sequence).toList());
  }
}
