package org.openbear.tool.autotest.reporting;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.openbear.tool.autotest.core.config.ReportingConfig;
import org.openbear.tool.autotest.core.util.ByteSizeParser;

final class EvidenceSanitizer {
  private final ObjectMapper mapper;
  private final Set<String> redacted;
  private final long maxBytes;

  EvidenceSanitizer(ObjectMapper mapper, ReportingConfig config) {
    this.mapper = mapper;
    this.redacted = new HashSet<>();
    config.getRedactedFields().forEach(f -> redacted.add(f.toLowerCase(Locale.ROOT)));
    this.maxBytes = ByteSizeParser.parse(config.getPayloadMaxSize());
  }

  JsonNode sanitize(Object value) {
    return walk(mapper.valueToTree(value), null);
  }

  private JsonNode walk(JsonNode node, String field) {
    if (field != null && sensitive(field)) return TextNode.valueOf("<redacted>");
    if (node == null || node.isNull()) return NullNode.getInstance();
    if (node.isTextual()) {
      byte[] bytes = node.asText().getBytes(StandardCharsets.UTF_8);
      if (bytes.length > maxBytes)
        return TextNode.valueOf(
            node.asText()
                    .substring(
                        0,
                        Math.min(
                            node.asText().length(), (int) Math.min(Integer.MAX_VALUE, maxBytes)))
                + "… <truncated>");
      return node;
    }
    if (node.isObject()) {
      ObjectNode out = mapper.createObjectNode();
      node.fields().forEachRemaining(e -> out.set(e.getKey(), walk(e.getValue(), e.getKey())));
      return out;
    }
    if (node.isArray()) {
      ArrayNode out = mapper.createArrayNode();
      node.forEach(child -> out.add(walk(child, field)));
      return out;
    }
    return node;
  }

  private boolean sensitive(String key) {
    String normalized = key.toLowerCase(Locale.ROOT);
    return redacted.stream().anyMatch(normalized::contains);
  }
}
