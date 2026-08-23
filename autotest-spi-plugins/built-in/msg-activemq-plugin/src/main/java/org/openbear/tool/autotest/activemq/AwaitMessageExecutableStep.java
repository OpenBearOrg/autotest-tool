package org.openbear.tool.autotest.activemq;

import java.util.Map;
import org.openbear.tool.autotest.spi.step.ExecutableStep;
import org.openbear.tool.autotest.spi.step.StepIdentity;

/** Immutable public-SPI message observation step. */
public record AwaitMessageExecutableStep(
    StepIdentity identity,
    String connection,
    String destination,
    String observationMode,
    String selector,
    Map<String, Object> polling,
    Map<String, Object> match,
    Map<String, Object> expect,
    Map<String, Object> capture)
    implements ExecutableStep {
  public AwaitMessageExecutableStep {
    polling = polling == null ? Map.of() : Map.copyOf(polling);
    match = match == null ? Map.of() : Map.copyOf(match);
    expect = expect == null ? Map.of() : Map.copyOf(expect);
    capture = capture == null ? Map.of() : Map.copyOf(capture);
  }

  @Override
  public String type() {
    return "awaitMessage";
  }
}
