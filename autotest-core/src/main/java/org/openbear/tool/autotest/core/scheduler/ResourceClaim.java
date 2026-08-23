package org.openbear.tool.autotest.core.scheduler;

import java.util.Objects;

public record ResourceClaim(ResourceLockKey key, ResourceLockMode mode) {
  public ResourceClaim {
    key = Objects.requireNonNull(key, "key");
    mode = Objects.requireNonNull(mode, "mode");
  }
}
