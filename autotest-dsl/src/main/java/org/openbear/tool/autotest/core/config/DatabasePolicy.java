package org.openbear.tool.autotest.core.config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DatabasePolicy {
  private boolean allowWrites = false;
}
