package org.openbear.tool.autotest.core.engine;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

final class ScenarioVariables implements Variables {
  private final Map<String, Object> values = new LinkedHashMap<>();

  ScenarioVariables(Map<String, Object> initialValues) {
    if (initialValues != null) values.putAll(initialValues);
  }

  void materializeRuntimeExpressions() {
    snapshot()
        .forEach(
            (name, value) -> {
              if (VariableResolver.isExactRuntimeExpression(value))
                put(name, VariableResolver.resolve(value, this));
            });
  }

  @Override
  public synchronized Optional<Object> find(String name) {
    return Optional.ofNullable(values.get(name));
  }

  @Override
  public synchronized Object require(String name) {
    Objects.requireNonNull(name, "name");
    if (!values.containsKey(name) || values.get(name) == null)
      throw new IllegalArgumentException("Variable is not defined: " + name);
    return values.get(name);
  }

  @Override
  public synchronized void put(String name, Object value) {
    values.put(Objects.requireNonNull(name, "name"), value);
  }

  @Override
  public synchronized Map<String, Object> snapshot() {
    return new LinkedHashMap<>(values);
  }
}
