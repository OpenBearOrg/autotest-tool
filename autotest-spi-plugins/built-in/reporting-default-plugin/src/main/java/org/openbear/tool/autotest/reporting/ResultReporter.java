package org.openbear.tool.autotest.reporting;

import java.io.IOException;
import java.nio.file.Path;
import org.openbear.tool.autotest.core.model.RunResult;

@FunctionalInterface
public interface ResultReporter {
  ReportArtifact write(RunResult result, Path outputDirectory, ReportingOptions options)
      throws IOException;
}
