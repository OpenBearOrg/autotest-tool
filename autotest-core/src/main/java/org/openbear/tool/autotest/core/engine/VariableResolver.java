package org.openbear.tool.autotest.core.engine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VariableResolver {
  private static final Pattern TOKEN = Pattern.compile("\\$\\{([^}]+)}");
  private static final RuntimeExpressionResolver EXPRESSIONS = RuntimeExpressionResolver.standard();

  public static List<String> references(String input) {
    List<String> refs = new ArrayList<>();
    if (input == null) return refs;
    Matcher m = TOKEN.matcher(input);
    while (m.find()) refs.add(m.group(1));
    return refs;
  }

  /** Resolves variables and runtime expressions recursively while preserving exact-token types. */
  public static Object resolve(
      Object value, org.openbear.tool.autotest.spi.service.Variables variables) {
    return resolve(value, variables, new LinkedHashSet<>());
  }

  private static Object resolve(
      Object value,
      org.openbear.tool.autotest.spi.service.Variables variables,
      Set<String> resolving) {
    if (value instanceof String text) return resolveText(text, variables, resolving);
    if (value instanceof Map<?, ?> map) {
      Map<String, Object> resolved = new LinkedHashMap<>();
      map.forEach(
          (key, item) -> resolved.put(String.valueOf(key), resolve(item, variables, resolving)));
      return resolved;
    }
    if (value instanceof List<?> list)
      return list.stream().map(item -> resolve(item, variables, resolving)).toList();
    return value;
  }

  public static boolean isExactRuntimeExpression(Object value) {
    if (!(value instanceof String text)) return false;
    Matcher matcher = TOKEN.matcher(text);
    return matcher.matches() && RuntimeExpressionResolver.isReserved(matcher.group(1));
  }

  private static Object resolveText(
      String text,
      org.openbear.tool.autotest.spi.service.Variables variables,
      Set<String> resolving) {
    Matcher matcher = TOKEN.matcher(text);
    if (!matcher.find()) return text;
    if (matcher.start() == 0 && matcher.end() == text.length())
      return resolveToken(matcher.group(1), variables, resolving);

    StringBuffer resolved = new StringBuffer();
    do {
      matcher.appendReplacement(
          resolved,
          Matcher.quoteReplacement(
              String.valueOf(resolveToken(matcher.group(1), variables, resolving))));
    } while (matcher.find());
    matcher.appendTail(resolved);
    return resolved.toString();
  }

  private static Object resolveToken(
      String token,
      org.openbear.tool.autotest.spi.service.Variables variables,
      Set<String> resolving) {
    return RuntimeExpressionResolver.isReserved(token)
        ? EXPRESSIONS.evaluate(token)
        : resolveVariable(token, variables, resolving);
  }

  private static Object resolveVariable(
      String name,
      org.openbear.tool.autotest.spi.service.Variables variables,
      Set<String> resolving) {
    if (!resolving.add(name))
      throw new IllegalArgumentException(
          "Circular variable reference: " + resolving + " -> " + name);
    try {
      return resolve(variables.require(name), variables, resolving);
    } finally {
      resolving.remove(name);
    }
  }
}
