package org.openbear.tool.autotest.core.model;

import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PollObservation {
  private Instant timestamp;
  private long elapsedMs;
  private Object value;

  public PollObservation() {}

  public PollObservation(Instant timestamp, long elapsedMs, Object value) {
    this.timestamp = timestamp;
    this.elapsedMs = elapsedMs;
    this.value = value;
  }
}
