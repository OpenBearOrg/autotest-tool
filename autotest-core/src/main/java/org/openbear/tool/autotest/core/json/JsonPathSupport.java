package org.openbear.tool.autotest.core.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JsonPathSupport {
  private final ObjectMapper mapper;
  private final Configuration configuration;

  public JsonPathSupport(ObjectMapper mapper) {
    this.mapper = mapper;
    this.configuration =
        Configuration.builder()
            .jsonProvider(new JacksonJsonProvider(mapper))
            .mappingProvider(new JacksonMappingProvider(mapper))
            .options(Option.DEFAULT_PATH_LEAF_TO_NULL)
            .build();
  }

  private static boolean looksLikeJson(String s) {
    String t = s.trim();
    return (t.startsWith("{") && t.endsWith("}")) || (t.startsWith("[") && t.endsWith("]"));
  }

  private Object coerceJsonStrings(Object value) throws JsonProcessingException {
    if (value == null) return null;
    if (value instanceof String s && looksLikeJson(s)) {
      try {
        return coerceJsonStrings(mapper.readValue(s, Object.class));
      } catch (JsonProcessingException ignored) {
        return s;
      }
    }
    if (value instanceof Map<?, ?> map) {
      Map<String, Object> out = new LinkedHashMap<>();
      for (Map.Entry<?, ?> entry : map.entrySet())
        out.put(String.valueOf(entry.getKey()), coerceJsonStrings(entry.getValue()));
      return out;
    }
    if (value instanceof Collection<?> collection) {
      List<Object> out = new ArrayList<>(collection.size());
      for (Object item : collection) out.add(coerceJsonStrings(item));
      return out;
    }
    return value;
  }

  public Object read(Object source, String path) {
    if (path == null || path.isBlank()) return source;
    try {
      Object prepared = coerceJsonStrings(mapper.convertValue(source, Object.class));
      String json = mapper.writeValueAsString(prepared);
      DocumentContext documentContext = JsonPath.using(configuration).parse(json);
      Object value = documentContext.read(path);
      return value instanceof JsonNode node ? mapper.convertValue(node, Object.class) : value;
    } catch (RuntimeException | JsonProcessingException e) {
      throw new IllegalArgumentException(
          "Unable to evaluate JSONPath '" + path + "': " + e.getMessage(), e);
    }
  }

  public Object readCapture(Object source, String path) {
    return unwrapSingletonCollection(read(source, path));
  }

  private static Object unwrapSingletonCollection(Object value) {
    while (value instanceof Collection<?> collection && collection.size() == 1)
      value = collection.iterator().next();
    return value;
  }

  public JsonNode toNode(Object value) {
    return mapper.valueToTree(value);
  }

  public ObjectMapper mapper() {
    return mapper;
  }
}
