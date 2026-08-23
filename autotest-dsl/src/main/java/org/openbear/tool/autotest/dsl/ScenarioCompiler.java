package org.openbear.tool.autotest.dsl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.openbear.tool.autotest.core.model.HttpStepDefinition;
import org.openbear.tool.autotest.core.model.ScenarioDefinition;
import org.openbear.tool.autotest.core.model.ScenarioPlan;
import org.openbear.tool.autotest.core.model.SqlStepDefinition;
import org.openbear.tool.autotest.core.model.StepDefinition;
import org.openbear.tool.autotest.core.util.Checksum;
import org.openbear.tool.autotest.core.util.Workspace;

public class ScenarioCompiler {
  private final Workspace workspace;
  private final ObjectMapper yamlMapper = ObjectMappers.yaml();
  private final ObjectMapper jsonMapper = ObjectMappers.json();
  private final SchemaValidator schema = new SchemaValidator(yamlMapper);
  private final SemanticValidator semantics;
  private final boolean allowDynamicSteps;

  public ScenarioCompiler(Workspace workspace) {
    this(workspace, false);
  }

  /** Parses scenario metadata while leaving step bodies for public-SPI provider compilation. */
  public ScenarioCompiler(Workspace workspace, boolean allowDynamicSteps) {
    this.workspace = workspace;
    this.semantics = new SemanticValidator(workspace, jsonMapper);
    this.allowDynamicSteps = allowDynamicSteps;
  }

  public ScenarioPlan compile(Path path) {
    Path absolute =
        path.isAbsolute() ? path.normalize() : workspace.root().resolve(path).normalize();
    if (!absolute.startsWith(workspace.root()))
      throw new ValidationException("Scenario escapes workspace", List.of(path.toString()));
    if (!Files.exists(absolute))
      throw new ValidationException("Scenario file does not exist", List.of(path.toString()));
    try {
      String text = Files.readString(absolute);
      ScenarioDefinition scenario;
      if (allowDynamicSteps) {
        ObjectNode root = (ObjectNode) yamlMapper.readTree(text);
        validateDynamicEnvelope(root);
        ObjectNode metadata = root.deepCopy();
        metadata.remove(List.of("setup", "steps", "cleanup"));
        scenario = yamlMapper.treeToValue(metadata, ScenarioDefinition.class);
      } else {
        schema.validate(text, "schema/scenario-1.0.0.schema.json");
        scenario = yamlMapper.readValue(text, ScenarioDefinition.class);
        semantics.validate(scenario);
      }
      Map<String, String> resources = resourceChecksums(scenario);
      return new ScenarioPlan(scenario, absolute, Checksum.sha256(absolute), resources);
    } catch (ValidationException e) {
      throw e;
    } catch (Exception e) {
      throw new ValidationException("Unable to compile scenario " + path, List.of(e.getMessage()));
    }
  }

  private static void validateDynamicEnvelope(ObjectNode root) {
    List<String> errors = new ArrayList<>();
    if (root == null) errors.add("Scenario must be an object");
    else {
      if (!root.path("dslVersion").isTextual()) errors.add("dslVersion is required");
      if (!root.path("id").isTextual() || root.path("id").asText().isBlank())
        errors.add("Scenario id is required");
      if (!root.path("name").isTextual() || root.path("name").asText().isBlank())
        errors.add("Scenario name is required");
      if (!root.path("steps").isArray() || root.path("steps").isEmpty())
        errors.add("Scenario must contain at least one step");
      for (String phase : List.of("setup", "steps", "cleanup"))
        if (root.has(phase) && !root.path(phase).isArray()) errors.add(phase + " must be an array");
    }
    if (!errors.isEmpty()) throw new ValidationException("Scenario is invalid", errors);
  }

  public List<ScenarioPlan> discover() {
    Path root = workspace.resolve("scenarios");
    if (!Files.exists(root)) return List.of();
    try (var stream = Files.walk(root)) {
      List<ScenarioPlan> plans =
          stream
              .filter(Files::isRegularFile)
              .filter(p -> p.toString().endsWith(".yaml") || p.toString().endsWith(".yml"))
              .sorted()
              .map(this::compile)
              .toList();
      Map<String, Path> seen = new LinkedHashMap<>();
      List<String> duplicates = new ArrayList<>();
      for (ScenarioPlan plan : plans) {
        Path previous = seen.putIfAbsent(plan.scenario().getId(), plan.sourcePath());
        if (previous != null)
          duplicates.add(plan.scenario().getId() + ": " + previous + " and " + plan.sourcePath());
      }
      if (!duplicates.isEmpty())
        throw new ValidationException("Duplicate scenario ids", duplicates);
      return plans;
    } catch (IOException e) {
      throw new ValidationException("Unable to discover scenarios", List.of(e.getMessage()));
    }
  }

  public ScenarioPlan find(String idOrPath) {
    if (idOrPath.endsWith(".yaml") || idOrPath.endsWith(".yml") || idOrPath.contains("/"))
      return compile(workspace.resolve(idOrPath));
    return discover().stream()
        .filter(p -> idOrPath.equals(p.scenario().getId()))
        .findFirst()
        .orElseThrow(() -> new ValidationException("Scenario id not found", List.of(idOrPath)));
  }

  private Map<String, String> resourceChecksums(ScenarioDefinition s) throws IOException {
    LinkedHashMap<String, String> out = new LinkedHashMap<>();
    List<StepDefinition> all = new ArrayList<>();
    all.addAll(s.getSetup());
    all.addAll(s.getSteps());
    all.addAll(s.getCleanup());
    for (StepDefinition step : all) {
      if (step instanceof HttpStepDefinition h && h.getRequest().getBodyFile() != null)
        add(out, h.getRequest().getBodyFile());
      if (step instanceof SqlStepDefinition q && q.getQueryFile() != null)
        add(out, q.getQueryFile());
    }
    return out;
  }

  private void add(Map<String, String> out, String relative) throws IOException {
    out.put(relative, Checksum.sha256(workspace.resolve(relative)));
  }
}
