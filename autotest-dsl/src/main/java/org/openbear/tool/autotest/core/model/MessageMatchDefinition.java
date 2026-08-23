package org.openbear.tool.autotest.core.model;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MessageMatchDefinition {
  private Object correlationId;
  private Object messageId;
  private Map<String, Object> properties = new LinkedHashMap<>();
  private Map<String, Object> body = new LinkedHashMap<>();

  public void setProperties(Map<String, Object> properties) {
    this.properties = properties == null ? new LinkedHashMap<>() : properties;
  }

  public void setBody(Map<String, Object> body) {
    this.body = body == null ? new LinkedHashMap<>() : body;
  }

  public boolean selectorConvertible() {
    return correlationId != null || !properties.isEmpty();
  }
}
