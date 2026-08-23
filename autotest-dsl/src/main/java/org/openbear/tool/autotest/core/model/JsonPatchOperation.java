package org.openbear.tool.autotest.core.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JsonPatchOperation {
  private String op;
  private String path;
  private Object value;
}
