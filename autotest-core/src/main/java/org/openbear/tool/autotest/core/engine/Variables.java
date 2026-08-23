package org.openbear.tool.autotest.core.engine;

import java.util.Map;
import java.util.Optional;

public interface Variables extends org.openbear.tool.autotest.spi.service.Variables {
  @Override
  Optional<Object> find(String name);

  @Override
  Object require(String name);

  @Override
  void put(String name, Object value);

  @Override
  Map<String, Object> snapshot();
}
