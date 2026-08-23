package org.openbear.tool.autotest.spi.plugin;

import java.util.Objects;

public record CapabilityDescriptor<T>(String id, Class<T> type) {
  public CapabilityDescriptor {
    id = Objects.requireNonNull(id, "id");
    type = Objects.requireNonNull(type, "type");
    if (id.isBlank()) throw new IllegalArgumentException("id must not be blank");
  }
}
