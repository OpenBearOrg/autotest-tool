package org.openbear.tool.autotest.reporting;

import java.nio.file.Path;
import java.util.Objects;

public record ReportArtifact(String type, Path path) {
  public ReportArtifact {
    type = Objects.requireNonNull(type, "type");
    path = Objects.requireNonNull(path, "path");
  }
}
