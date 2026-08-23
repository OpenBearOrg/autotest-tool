package org.openbear.tool.autotest.core.engine;

import java.util.List;
import java.util.Objects;
import org.openbear.tool.autotest.core.domain.RunId;
import org.openbear.tool.autotest.core.event.ExecutionListener;

public record RunRequest(RunId runId, String label, List<ExecutionListener> listeners) {
  public RunRequest {
    runId = Objects.requireNonNull(runId, "runId");
    listeners = listeners == null ? List.of() : List.copyOf(listeners);
  }

  public static RunRequest create(RunId runId, String label) {
    return new RunRequest(runId, label, List.of());
  }
}
