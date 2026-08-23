package org.openbear.tool.autotest.core.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class ImmutableValues {
  private ImmutableValues() {}

  static Map<String, Object> map(Map<String, Object> values) {
    if (values == null || values.isEmpty()) return Map.of();
    LinkedHashMap<String, Object> copy = new LinkedHashMap<>();
    values.forEach((key, value) -> copy.put(key, copyValue(value)));
    return Collections.unmodifiableMap(copy);
  }

  static Map<String, String> strings(Map<String, String> values) {
    if (values == null || values.isEmpty()) return Map.of();
    return Collections.unmodifiableMap(new LinkedHashMap<>(values));
  }

  static <T> List<T> list(List<T> values) {
    if (values == null || values.isEmpty()) return List.of();
    return Collections.unmodifiableList(new ArrayList<>(values));
  }

  static <T> Set<T> set(Iterable<T> values) {
    if (values == null) return Set.of();
    LinkedHashSet<T> copy = new LinkedHashSet<>();
    for (T value : values) copy.add(value);
    return Collections.unmodifiableSet(copy);
  }

  private static Object copyValue(Object value) {
    if (value instanceof Map<?, ?> map) {
      LinkedHashMap<Object, Object> copy = new LinkedHashMap<>();
      map.forEach((key, nested) -> copy.put(key, copyValue(nested)));
      return Collections.unmodifiableMap(copy);
    }
    if (value instanceof List<?> list) {
      List<Object> copy = new ArrayList<>(list.size());
      list.forEach(item -> copy.add(copyValue(item)));
      return Collections.unmodifiableList(copy);
    }
    if (value instanceof Set<?> set) {
      LinkedHashSet<Object> copy = new LinkedHashSet<>();
      set.forEach(item -> copy.add(copyValue(item)));
      return Collections.unmodifiableSet(copy);
    }
    return value;
  }
}
