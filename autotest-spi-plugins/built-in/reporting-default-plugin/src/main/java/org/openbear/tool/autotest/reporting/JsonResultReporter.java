package org.openbear.tool.autotest.reporting;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.openbear.tool.autotest.core.model.RunResult;

public final class JsonResultReporter implements ResultReporter {
  private final ObjectMapper mapper =
      new ObjectMapper()
          .findAndRegisterModules()
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

  @Override
  public ReportArtifact write(RunResult result, Path outputDirectory, ReportingOptions options)
      throws IOException {
    EvidenceSanitizer sanitizer = new EvidenceSanitizer(mapper, options.config());
    JsonNode clean = sanitizer.sanitize(result);
    if (clean instanceof ObjectNode object) object.put("schemaVersion", "1.0");
    byte[] json = mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(clean);
    Path resultPath = outputDirectory.resolve("result.json");
    Files.write(resultPath, json);
    Files.write(outputDirectory.resolve("run.json"), json);
    return new ReportArtifact("json", resultPath);
  }
}
