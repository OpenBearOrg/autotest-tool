package org.openbear.tool.autotest.reporting;

import java.io.IOException;
import java.nio.file.Path;
import org.openbear.tool.autotest.core.model.RunResult;

public final class HtmlResultReporter implements ResultReporter {
  @Override
  public ReportArtifact write(RunResult result, Path outputDirectory, ReportingOptions options)
      throws IOException {
    Path path =
        new DefaultRunReportWriter().writeHtmlBundle(result, outputDirectory, options.config());
    return new ReportArtifact("html", path.resolve("index.html"));
  }
}
