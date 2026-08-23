package org.openbear.tool.autotest.spi.service;

import java.util.Map;
import java.util.Optional;

public interface Variables {
  Optional<Object> find(String name);

  Object require(String name);

  void put(String name, Object value);

  Map<String, Object> snapshot();
}
