package org.openbear.tool.autotest.core.event;

import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ExecutionEventDispatcher {
  private static final Logger LOGGER = LoggerFactory.getLogger(ExecutionEventDispatcher.class);

  private final AtomicLong sequence = new AtomicLong();
  private final Clock clock;
  private final List<ExecutionListener> listeners;

  public ExecutionEventDispatcher(Clock clock, List<? extends ExecutionListener> listeners) {
    this.clock = Objects.requireNonNull(clock, "clock");
    this.listeners = listeners == null ? List.of() : List.copyOf(listeners);
  }

  public void publish(ExecutionEvent event) {
    ExecutionEventEnvelope envelope =
        new ExecutionEventEnvelope(sequence.incrementAndGet(), clock.instant(), event);
    for (ExecutionListener listener : listeners) {
      try {
        listener.onEvent(envelope);
      } catch (RuntimeException e) {
        LOGGER.warn("Execution listener {} failed", listener.getClass().getName(), e);
      }
    }
  }
}
