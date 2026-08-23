package org.openbear.tool.autotest.dsl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.openbear.tool.autotest.core.AutotestVersion;
import org.openbear.tool.autotest.core.engine.RuntimeExpressionResolver;
import org.openbear.tool.autotest.core.model.AwaitMessageStepDefinition;
import org.openbear.tool.autotest.core.model.AwaitSqlStepDefinition;
import org.openbear.tool.autotest.core.model.HttpStepDefinition;
import org.openbear.tool.autotest.core.model.ScenarioDefinition;
import org.openbear.tool.autotest.core.model.SetStepDefinition;
import org.openbear.tool.autotest.core.model.SqlStepDefinition;
import org.openbear.tool.autotest.core.model.StepDefinition;
import org.openbear.tool.autotest.core.util.DurationParser;
import org.openbear.tool.autotest.core.util.Workspace;

public class SemanticValidator {
  private final Workspace workspace;
  private final VariableReferenceScanner refs;
  private final ObjectMapper mapper;

  public SemanticValidator(Workspace workspace, ObjectMapper mapper) {
    this.workspace = workspace;
    this.mapper = mapper;
    this.refs = new VariableReferenceScanner(mapper);
  }

  private static boolean blank(String s) {
    return s == null || s.isBlank();
  }

  public void validate(ScenarioDefinition scenario) {
    List<String> errors = new ArrayList<>();
    if (!AutotestVersion.DSL_VERSION.equals(scenario.getDslVersion()))
      errors.add("Unsupported dslVersion: " + scenario.getDslVersion());
    if (blank(scenario.getId())) errors.add("Scenario id is required");
    if (blank(scenario.getName())) errors.add("Scenario name is required");
    if (scenario.getSteps().isEmpty()) errors.add("Scenario must contain at least one step");

    Set<String> ids = new HashSet<>();
    Set<String> available = new HashSet<>(scenario.getVariables().keySet());
    available.addAll(scenario.getRequiredVariables());
    available.addAll(List.of("runId", "executionId", "scenarioId", "environment"));
    validateRuntimeExpressions(mapper.valueToTree(scenario.getVariables()), "variables", errors);
    validatePhase("setup", scenario.getSetup(), ids, available, errors);
    validatePhase("steps", scenario.getSteps(), ids, available, errors);
    validatePhase("cleanup", scenario.getCleanup(), ids, available, errors);
    if (!errors.isEmpty())
      throw new ValidationException("Scenario " + scenario.getId() + " is invalid", errors);
  }

  private void validatePhase(
      String phase,
      List<StepDefinition> steps,
      Set<String> ids,
      Set<String> available,
      List<String> errors) {
    int index = 0;
    for (StepDefinition step : steps) {
      index++;
      String loc = phase + "[" + index + "]";
      if (blank(step.getId())) errors.add(loc + ": step id is required");
      else if (!ids.add(step.getId())) errors.add(loc + ": duplicate step id " + step.getId());
      validateStep(step, loc, errors);
      validateReferences(step, available, loc, errors);
      if (step instanceof HttpStepDefinition h) available.addAll(h.getCapture().keySet());
      if (step instanceof SqlStepDefinition s) available.addAll(s.getCapture().keySet());
      if (step instanceof AwaitMessageStepDefinition m) available.addAll(m.getCapture().keySet());
      if (step instanceof SetStepDefinition s) available.addAll(s.getValues().keySet());
    }
  }

  private void validateReferences(
      StepDefinition step, Set<String> available, String loc, List<String> errors) {
    JsonNode node = mapper.valueToTree(step);
    if (!node.isObject()) {
      checkReferences(node, available, referenceLoc(step, loc), errors);
      return;
    }
    ObjectNode root = (ObjectNode) node;
    ObjectNode payload = unwrapStep(root);
    JsonNode captureNode = payload.get("capture");
    Set<String> stepCaptures = captureNames(captureNode);
    ObjectNode withoutCapture = payload.deepCopy();
    withoutCapture.remove("capture");
    checkReferences(
        withoutCapture, union(available, stepCaptures), referenceLoc(step, loc), errors);
    if (captureNode != null)
      checkReferences(captureNode, available, referenceLoc(step, loc), errors);
  }

  private String referenceLoc(StepDefinition step, String loc) {
    return blank(step.getId()) ? loc : loc + " " + step.getId();
  }

  private ObjectNode unwrapStep(ObjectNode node) {
    if (node.size() != 1) return node;
    JsonNode first = node.elements().next();
    return first instanceof ObjectNode payload ? payload : node;
  }

  private void checkReferences(
      JsonNode node, Set<String> available, String loc, List<String> errors) {
    validateRuntimeExpressions(node, loc, errors);
    for (String ref : refs.references(node)) {
      if (ref.startsWith("ENV:")) continue;
      if (!RuntimeExpressionResolver.isReserved(ref) && !available.contains(ref))
        errors.add(loc + ": variable '" + ref + "' is not declared/captured before use");
    }
  }

  private void validateRuntimeExpressions(JsonNode node, String loc, List<String> errors) {
    for (String ref : refs.references(node)) {
      if (!RuntimeExpressionResolver.isReserved(ref)) continue;
      try {
        RuntimeExpressionResolver.validate(ref);
      } catch (IllegalArgumentException e) {
        errors.add(loc + ": " + e.getMessage());
      }
    }
  }

  private Set<String> captureNames(JsonNode captureNode) {
    Set<String> names = new HashSet<>();
    if (captureNode == null || !captureNode.isObject()) return names;
    captureNode.fieldNames().forEachRemaining(names::add);
    return names;
  }

  private Set<String> union(Set<String> left, Set<String> right) {
    Set<String> out = new HashSet<>(left);
    out.addAll(right);
    return out;
  }

  private void validateStep(StepDefinition step, String loc, List<String> errors) {
    try {
      if (step instanceof HttpStepDefinition h) {
        if (blank(h.getService())) errors.add(loc + ": http.service is required");
        if (h.getRequest().getBodyFile() != null && h.getRequest().getBody() != null)
          errors.add(loc + ": request.body and request.bodyFile are mutually exclusive");
        if (h.getRequest().getBodyFile() != null) {
          resource(h.getRequest().getBodyFile(), loc, errors);
          try {
            mapper.readTree(workspace.readText(h.getRequest().getBodyFile()));
          } catch (Exception e) {
            errors.add(loc + ": request bodyFile is not valid JSON: " + e.getMessage());
          }
        }
        h.getRequest()
            .getPatch()
            .forEach(
                p -> {
                  if (p.getPath() == null
                      || (!p.getPath().isEmpty() && !p.getPath().startsWith("/")))
                    errors.add(loc + ": patch path must be JSON Pointer syntax: " + p.getPath());
                  if (!Set.of("add", "replace", "remove")
                      .contains(String.valueOf(p.getOp()).toLowerCase(Locale.ROOT)))
                    errors.add(loc + ": unsupported patch op " + p.getOp());
                });
        if (h.getRequest().getTimeout() != null) DurationParser.parse(h.getRequest().getTimeout());
      }
      if (step instanceof SqlStepDefinition s) {
        if (blank(s.getConnection())) errors.add(loc + ": sql.connection is required");
        if (s.getQueryFile() != null && s.getQuery() != null)
          errors.add(loc + ": query and queryFile are mutually exclusive");
        if (s.getQueryFile() == null && blank(s.getQuery()))
          errors.add(loc + ": query or queryFile is required");
        if (s.getQueryFile() != null) {
          resource(s.getQueryFile(), loc, errors);
          try {
            if (workspace.readText(s.getQueryFile()).isBlank())
              errors.add(loc + ": queryFile is empty: " + s.getQueryFile());
          } catch (Exception ignored) {
          }
        }
      }
      if (step instanceof AwaitSqlStepDefinition a) validatePolling(a.getPolling(), loc, errors);
      if (step instanceof AwaitMessageStepDefinition m) {
        if (blank(m.getConnection())) errors.add(loc + ": awaitMessage.connection is required");
        if (blank(m.getDestination())) errors.add(loc + ": awaitMessage.destination is required");
        if (!Set.of("dedicated", "browse")
            .contains(String.valueOf(m.getObservationMode()).toLowerCase(Locale.ROOT)))
          errors.add(loc + ": observationMode must be dedicated or browse");
        if ("dedicated".equalsIgnoreCase(m.getObservationMode())
            && blank(m.getSelector())
            && !m.getMatch().selectorConvertible())
          errors.add(
              loc
                  + ": dedicated observation requires selector, correlationId, or message properties to avoid consuming unrelated "
                  + "messages");
        validatePolling(m.getPolling(), loc, errors);
      }
    } catch (IllegalArgumentException e) {
      errors.add(loc + ": " + e.getMessage());
    }
  }

  private void validatePolling(
      org.openbear.tool.autotest.core.config.PollingConfig p, String loc, List<String> errors) {
    if (p == null) return;
    if (p.getTimeout() != null && !DurationParser.parse(p.getTimeout()).isPositive())
      errors.add(loc + ": polling.timeout must be > 0");
    if (p.getInterval() != null && !DurationParser.parse(p.getInterval()).isPositive())
      errors.add(loc + ": polling.interval must be > 0");
  }

  private void resource(String relative, String loc, List<String> errors) {
    try {
      if (!Files.exists(workspace.resolve(relative)))
        errors.add(loc + ": resource does not exist: " + relative);
    } catch (Exception e) {
      errors.add(loc + ": invalid resource path: " + relative);
    }
  }
}
