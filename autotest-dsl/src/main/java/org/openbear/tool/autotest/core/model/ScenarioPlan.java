package org.openbear.tool.autotest.core.model;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class ScenarioPlan {
  private final ScenarioDefinition scenario;
  private final Path sourcePath;
  private final String checksum;
  private final Map<String, String> resourceChecksums;

  public ScenarioPlan(
      ScenarioDefinition scenario,
      Path sourcePath,
      String checksum,
      Map<String, String> resourceChecksums) {
    this.scenario = scenario;
    this.sourcePath = sourcePath;
    this.checksum = checksum;
    this.resourceChecksums = new LinkedHashMap<>(resourceChecksums);
  }

  public ScenarioDefinition scenario() {
    return scenario;
  }

  public Path sourcePath() {
    return sourcePath;
  }

  public String checksum() {
    return checksum;
  }

  public Map<String, String> resourceChecksums() {
    return resourceChecksums;
  }
}
