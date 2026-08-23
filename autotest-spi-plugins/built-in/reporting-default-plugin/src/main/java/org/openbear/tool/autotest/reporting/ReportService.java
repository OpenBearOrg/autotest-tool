package org.openbear.tool.autotest.reporting;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.openbear.tool.autotest.core.model.RunResult;

public final class ReportService {
  private final List<ResultReporter> reporters;

  public ReportService() {
    this(List.of(new HtmlResultReporter(), new JsonResultReporter(), new JUnitXmlResultReporter()));
  }

  public ReportService(List<? extends ResultReporter> reporters) {
    this.reporters = List.copyOf(reporters);
  }

  public List<ReportArtifact> writeAll(
      RunResult result, Path outputDirectory, ReportingOptions options) throws IOException {
    Files.createDirectories(outputDirectory);
    Files.createDirectories(outputDirectory.resolve("evidence"));
    Files.createDirectories(outputDirectory.resolve("logs"));
    return reporters.stream()
        .map(
            reporter -> {
              try {
                return reporter.write(result, outputDirectory, options);
              } catch (IOException e) {
                throw new ReportWriteException(reporter.getClass().getSimpleName(), e);
              }
            })
        .toList();
  }

  public static final class ReportWriteException extends RuntimeException {
    public ReportWriteException(String reporter, IOException cause) {
      super("Failed to write report with " + reporter, cause);
    }
  }
}
