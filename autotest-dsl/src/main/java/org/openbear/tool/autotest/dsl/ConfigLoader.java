package org.openbear.tool.autotest.dsl;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.openbear.tool.autotest.core.AutotestVersion;
import org.openbear.tool.autotest.core.config.EnvironmentConfig;
import org.openbear.tool.autotest.core.config.ProjectConfig;
import org.openbear.tool.autotest.core.model.SuiteDefinition;
import org.openbear.tool.autotest.core.util.Workspace;

public class ConfigLoader {
  private final Workspace workspace;
  private final ObjectMapper mapper = ObjectMappers.yaml();
  private final SchemaValidator schema = new SchemaValidator(mapper);

  public ConfigLoader(Workspace workspace) {
    this.workspace = workspace;
  }

  public ProjectConfig loadProject() {
    Path p = workspace.resolve("autotest-tool.yaml");
    if (!Files.exists(p)) return new ProjectConfig();
    ProjectConfig cfg = read(p, "schema/project-1.0.0.schema.json", ProjectConfig.class);
    ConfigSemanticValidator.validate(cfg);
    return cfg;
  }

  public EnvironmentConfig loadEnvironment(String nameOrPath) {
    Path p =
        nameOrPath.endsWith(".yaml") || nameOrPath.endsWith(".yml")
            ? workspace.resolve(nameOrPath)
            : workspace.resolve("environments/" + nameOrPath + ".yaml");
    EnvironmentConfig cfg =
        read(p, "schema/environment-1.0.0.schema.json", EnvironmentConfig.class);
    if (!AutotestVersion.ENVIRONMENT_VERSION.equals(cfg.getEnvironmentVersion()))
      throw new ValidationException(
          "Unsupported environmentVersion", List.of(cfg.getEnvironmentVersion()));
    ConfigSemanticValidator.validate(cfg);
    return cfg;
  }

  public SuiteDefinition loadSuite(String nameOrPath) {
    Path p =
        nameOrPath.endsWith(".yaml") || nameOrPath.endsWith(".yml")
            ? workspace.resolve(nameOrPath)
            : workspace.resolve("suites/" + nameOrPath + ".yaml");
    SuiteDefinition suite = read(p, "schema/suite-1.0.0.schema.json", SuiteDefinition.class);
    if (!AutotestVersion.SUITE_VERSION.equals(suite.getSuiteVersion()))
      throw new ValidationException("Unsupported suiteVersion", List.of(suite.getSuiteVersion()));
    return suite;
  }

  private <T> T read(Path path, String schemaResource, Class<T> type) {
    if (!Files.exists(path))
      throw new ValidationException(
          "Configuration file does not exist",
          List.of(workspace.root().relativize(path).toString()));
    try {
      String text = Files.readString(path);
      schema.validate(text, schemaResource);
      return mapper.readValue(text, type);
    } catch (ValidationException e) {
      throw e;
    } catch (IOException e) {
      throw new ValidationException("Unable to read " + path, List.of(e.getMessage()));
    }
  }
}
