package org.openbear.tool.autotest.core.scheduler;

import java.util.Objects;

public record ResourceLockKey(String namespace, String name)
    implements Comparable<ResourceLockKey> {
  public ResourceLockKey {
    if (namespace == null || namespace.isBlank())
      throw new IllegalArgumentException("namespace is blank");
    if (name == null || name.isBlank()) throw new IllegalArgumentException("name is blank");
  }

  @Override
  public int compareTo(ResourceLockKey other) {
    int namespaceCompare = namespace.compareTo(other.namespace);
    return namespaceCompare != 0 ? namespaceCompare : name.compareTo(other.name);
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof ResourceLockKey key
        && namespace.equals(key.namespace)
        && name.equals(key.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(namespace, name);
  }
}
