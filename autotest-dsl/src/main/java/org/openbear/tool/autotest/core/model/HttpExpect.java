package org.openbear.tool.autotest.core.model;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HttpExpect {
  private Integer status;
  private Map<String, Object> values = new LinkedHashMap<>();

  public void setValues(Map<String, Object> values) {
    this.values = values == null ? new LinkedHashMap<>() : values;
  }
}
