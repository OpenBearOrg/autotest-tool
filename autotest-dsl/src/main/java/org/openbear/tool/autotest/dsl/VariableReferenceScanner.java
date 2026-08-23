package org.openbear.tool.autotest.dsl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.openbear.tool.autotest.core.engine.VariableResolver;
import org.openbear.tool.autotest.core.model.StepDefinition;

public class VariableReferenceScanner {
  private final ObjectMapper mapper;

  public VariableReferenceScanner(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  public List<String> references(StepDefinition step) {
    return references(mapper.valueToTree(step));
  }

  public List<String> references(JsonNode node) {
    List<String> refs = new ArrayList<>();
    scan(node, refs);
    return refs;
  }

  private void scan(JsonNode node, List<String> refs) {
    if (node == null) return;
    if (node.isTextual()) {
      refs.addAll(VariableResolver.references(node.asText()));
    } else if (node.isArray()) node.forEach(n -> scan(n, refs));
    else if (node.isObject()) {
      Iterator<Map.Entry<String, JsonNode>> it = node.fields();
      while (it.hasNext()) {
        Map.Entry<String, JsonNode> e = it.next();
        refs.addAll(VariableResolver.references(e.getKey()));
        scan(e.getValue(), refs);
      }
    }
  }
}
