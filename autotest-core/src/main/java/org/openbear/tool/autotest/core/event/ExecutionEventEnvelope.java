package org.openbear.tool.autotest.core.event;

import java.time.Instant;
import java.util.Objects;

public record ExecutionEventEnvelope(long sequence, Instant timestamp, ExecutionEvent event) {
  public ExecutionEventEnvelope {
    if (sequence < 1) throw new IllegalArgumentException("sequence must be positive");
    timestamp = Objects.requireNonNull(timestamp, "timestamp");
    event = Objects.requireNonNull(event, "event");
  }
}
