package org.openbear.tool.autotest.core.json;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JsonPathSupportTest {
  private final JsonPathSupport support = new JsonPathSupport(new ObjectMapper());

  @Test
  void unwrapsSingletonCollectionsWhenReadingCaptures() {
    Map<String, Object> primaryResource = Map.of("id", "RESOURCE-A");
    Map<String, Object> primaryItem = Map.of("id", "0", "resource", primaryResource);
    Map<String, Object> secondaryResource = Map.of("id", "RESOURCE-B");
    Map<String, Object> secondaryItem = Map.of("id", "1", "resource", secondaryResource);
    Map<String, Object> source = Map.of("resources", List.of(primaryItem, secondaryItem));

    assertEquals(
        "0", support.readCapture(source, "$.resources[?(@.resource.id == 'RESOURCE-A')].id"));
    assertEquals(List.of("0", "1"), support.readCapture(source, "$.resources[*].id"));
  }
}
