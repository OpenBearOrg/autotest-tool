package org.openbear.tool.autotest.http;

import java.util.Map;
import org.openbear.tool.autotest.spi.step.ExecutableStep;
import org.openbear.tool.autotest.spi.step.StepIdentity;

/** Immutable HTTP step consumed by the public runtime. */
public record HttpExecutableStep(
    StepIdentity identity,
    String service,
    Map<String, Object> request,
    Map<String, Object> expect,
    Map<String, Object> capture)
    implements ExecutableStep {
  public HttpExecutableStep {
    request = request == null ? Map.of() : Map.copyOf(request);
    expect = expect == null ? Map.of() : Map.copyOf(expect);
    capture = capture == null ? Map.of() : Map.copyOf(capture);
  }

  @Override
  public String type() {
    return "http";
  }
}
