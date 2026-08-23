package org.openbear.tool.autotest.jdbc.step;

import java.util.LinkedHashMap;
import java.util.Map;
import org.openbear.tool.autotest.core.engine.VariableResolver;
import org.openbear.tool.autotest.jdbc.JdbcQueryResult;
import org.openbear.tool.autotest.jdbc.runtime.JdbcRuntime;
import org.openbear.tool.autotest.spi.step.ExecutableStep;
import org.openbear.tool.autotest.spi.step.StepExecutionContext;
import org.openbear.tool.autotest.spi.step.StepExecutionResult;
import org.openbear.tool.autotest.spi.step.StepHandler;

public final class SqlStepHandler implements StepHandler<SqlExecutableStep> {
  private final JdbcRuntime jdbc;

  public SqlStepHandler(JdbcRuntime jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Class<SqlExecutableStep> stepType() {
    return SqlExecutableStep.class;
  }

  @Override
  public StepExecutionResult execute(SqlExecutableStep step, StepExecutionContext context)
      throws Exception {
    JdbcQueryResult result = query(step, context);
    Map<String, Object> captures = capture(step, result, context);
    String failure = failure(step.expect(), result, context);
    Map<String, Object> evidence = Map.of("result", result.toEvidence());
    return failure == null
        ? StepExecutionResult.success(captures, evidence)
        : StepExecutionResult.failure(failure, evidence);
  }

  JdbcQueryResult query(SqlExecutableStep step, StepExecutionContext context) {
    String sql =
        step.queryFile() == null
            ? resolve(step.query(), context)
            : context.services().resources().readText(step.queryFile());
    return jdbc.execute(step.connection(), sql, resolveMap(step.parameters(), context));
  }

  static Map<String, Object> capture(
      ExecutableStep step, JdbcQueryResult result, StepExecutionContext context) {
    Map<String, Object> definitions = definitions(step);
    Map<String, Object> captures = new LinkedHashMap<>();
    for (Map.Entry<String, Object> entry : definitions.entrySet()) {
      if (!(entry.getValue() instanceof Map<?, ?> definition)) continue;
      Object path = definition.get("jsonPath");
      if (path == null) continue;
      Object value =
          context.services().json().read(result.toEvidence(), String.valueOf(path)).orElse(null);
      context.services().variables().put(entry.getKey(), value);
      captures.put(entry.getKey(), value);
    }
    return captures;
  }

  static String failure(
      Map<String, Object> expected, JdbcQueryResult result, StepExecutionContext context) {
    if (expected == null) return null;
    Object rowCount =
        VariableResolver.resolve(expected.get("rowCount"), context.services().variables());
    if (rowCount instanceof Number number && result.rowCount() != number.intValue())
      return "rowCount expected " + number.intValue() + " but was " + result.rowCount();
    Object values = expected.get("values");
    if (values instanceof Map<?, ?> map) {
      try {
        @SuppressWarnings("unchecked")
        Map<String, ?> typed = (Map<String, ?>) map;
        context.services().assertions().verifyValues(result.toEvidence(), typed);
      } catch (RuntimeException e) {
        return e.getMessage();
      }
    }
    return null;
  }

  static Map<String, Object> definitions(ExecutableStep step) {
    if (step instanceof SqlExecutableStep sql) return sql.capture();
    return ((AwaitSqlExecutableStep) step).capture();
  }

  static String resolve(String value, StepExecutionContext context) {
    if (value == null) return null;
    return String.valueOf(VariableResolver.resolve(value, context.services().variables()));
  }

  static Map<String, Object> resolveMap(Map<String, Object> values, StepExecutionContext context) {
    if (values == null) return Map.of();
    @SuppressWarnings("unchecked")
    Map<String, Object> resolved =
        (Map<String, Object>) VariableResolver.resolve(values, context.services().variables());
    return resolved;
  }
}
