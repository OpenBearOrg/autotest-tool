package org.openbear.tool.autotest.core.domain;

import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

public record ScenarioSource(
    Path path, String scenarioChecksum, Map<String, String> resourceChecksums) {
  public ScenarioSource {
    path = Objects.requireNonNull(path, "path").toAbsolutePath().normalize();
    scenarioChecksum = IdValue.requireNonBlank(scenarioChecksum, "scenarioChecksum");
    resourceChecksums = ImmutableValues.strings(resourceChecksums);
  }
}
