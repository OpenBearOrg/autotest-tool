package org.openbear.tool.autotest.core.domain;

import java.time.Duration;
import java.util.Objects;

public record PollingSettings(Duration timeout, Duration interval) {
  public PollingSettings {
    timeout = Objects.requireNonNull(timeout, "timeout");
    interval = Objects.requireNonNull(interval, "interval");
    if (timeout.isZero() || timeout.isNegative())
      throw new IllegalArgumentException("timeout must be > 0");
    if (interval.isZero() || interval.isNegative())
      throw new IllegalArgumentException("interval must be > 0");
  }
}
