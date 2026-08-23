package org.openbear.tool.autotest.dsl.compile;

import java.nio.file.Path;
import java.util.Objects;

public record ValidationRequest(Path workspace) {
  public ValidationRequest {
    workspace = Objects.requireNonNull(workspace, "workspace").toAbsolutePath().normalize();
  }
}
