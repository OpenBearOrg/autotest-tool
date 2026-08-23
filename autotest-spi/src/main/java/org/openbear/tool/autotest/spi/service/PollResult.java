package org.openbear.tool.autotest.spi.service;

import java.util.List;

public record PollResult<T>(boolean matched, T lastValue, List<Object> observations) {
  public PollResult {
    observations = observations == null ? List.of() : List.copyOf(observations);
  }
}
