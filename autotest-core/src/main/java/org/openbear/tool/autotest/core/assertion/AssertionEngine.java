package org.openbear.tool.autotest.core.assertion;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public class AssertionEngine {
  private static boolean isOperatorMap(Map<?, ?> m) {
    return m.keySet().stream()
        .map(String::valueOf)
        .anyMatch(
            k ->
                List.of(
                        "notNull",
                        "isNull",
                        "equals",
                        "contains",
                        "matches",
                        "in",
                        "greaterThan",
                        "lessThan")
                    .contains(k));
  }

  private static boolean numeric(Object o) {
    if (o instanceof Number) return true;
    if (o instanceof String s)
      try {
        new BigDecimal(s);
        return true;
      } catch (Exception ignored) {
      }
    return false;
  }

  private static BigDecimal decimal(Object o) {
    return new BigDecimal(String.valueOf(o));
  }

  private static Object normalize(Object o) {
    return o == null ? null : o;
  }

  private static Object unwrapSingletonCollection(Object value) {
    while (value instanceof Collection<?> collection && collection.size() == 1)
      value = collection.iterator().next();
    return value;
  }

  public String compare(Object actual, Object expected) {
    if (expected instanceof Map<?, ?> operators && isOperatorMap(operators))
      return compareOperators(actual, operators);
    actual = unwrapSingletonCollection(actual);
    if (numeric(actual) && numeric(expected)) {
      if (decimal(actual).compareTo(decimal(expected)) != 0)
        return "expected " + expected + " but was " + actual;
      return null;
    }
    if (!Objects.equals(normalize(actual), normalize(expected)))
      return "expected " + expected + " but was " + actual;
    return null;
  }

  private String compareOperators(Object actual, Map<?, ?> ops) {
    Object candidate =
        ops.containsKey("contains") || ops.containsKey("in")
            ? actual
            : unwrapSingletonCollection(actual);
    if (Boolean.TRUE.equals(ops.get("notNull")) && candidate == null)
      return "expected non-null value";
    if (Boolean.TRUE.equals(ops.get("isNull")) && candidate != null)
      return "expected null but was " + candidate;
    if (ops.containsKey("equals")) {
      String f = compare(candidate, ops.get("equals"));
      if (f != null) return f;
    }
    if (ops.containsKey("contains")) {
      Object needle = ops.get("contains");
      boolean ok =
          candidate instanceof String s && s.contains(String.valueOf(needle))
              || candidate instanceof Collection<?> c && c.contains(needle);
      if (!ok) return "expected value containing " + needle + " but was " + candidate;
    }
    if (ops.containsKey("matches")) {
      if (candidate == null
          || !Pattern.compile(String.valueOf(ops.get("matches")))
              .matcher(String.valueOf(candidate))
              .matches())
        return "expected value matching /" + ops.get("matches") + "/ but was " + candidate;
    }
    if (ops.containsKey("in")) {
      Object values = ops.get("in");
      if (!(values instanceof Collection<?> c) || !c.contains(candidate))
        return "expected one of " + values + " but was " + candidate;
    }
    if (ops.containsKey("greaterThan")) {
      if (!numeric(candidate) || decimal(candidate).compareTo(decimal(ops.get("greaterThan"))) <= 0)
        return "expected > " + ops.get("greaterThan") + " but was " + candidate;
    }
    if (ops.containsKey("lessThan")) {
      if (!numeric(candidate) || decimal(candidate).compareTo(decimal(ops.get("lessThan"))) >= 0)
        return "expected < " + ops.get("lessThan") + " but was " + candidate;
    }
    return null;
  }
}
