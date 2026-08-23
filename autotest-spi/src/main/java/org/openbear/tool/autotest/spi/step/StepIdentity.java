package org.openbear.tool.autotest.spi.step;

import java.util.Objects;

public record StepIdentity(String id, String name, String description, boolean continueOnFailure) {
  public StepIdentity {
    id = Objects.requireNonNull(id, "id");
    if (id.isBlank()) throw new IllegalArgumentException("id must not be blank");
  }

  public String displayName() {
    return name == null || name.isBlank() ? id : name;
  }
}
