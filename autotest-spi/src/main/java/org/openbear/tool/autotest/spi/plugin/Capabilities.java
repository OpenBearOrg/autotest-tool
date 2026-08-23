package org.openbear.tool.autotest.spi.plugin;

import java.util.List;
import java.util.Optional;

@FunctionalInterface
public interface Capabilities {
  <T> List<T> all(Class<T> type);

  default <T> Optional<T> single(Class<T> type) {
    List<T> values = all(type);
    if (values.size() > 1)
      throw new IllegalStateException("Multiple capabilities registered for " + type.getName());
    return values.stream().findFirst();
  }
}
