package org.openbear.tool.autotest.jdbc.step;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.openbear.tool.autotest.jdbc.JdbcDurations;
import org.openbear.tool.autotest.jdbc.JdbcQueryResult;
import org.openbear.tool.autotest.jdbc.runtime.JdbcRuntime;
import org.openbear.tool.autotest.spi.service.PollRequest;
import org.openbear.tool.autotest.spi.service.PollResult;
import org.openbear.tool.autotest.spi.step.StepExecutionContext;
import org.openbear.tool.autotest.spi.step.StepExecutionResult;
import org.openbear.tool.autotest.spi.step.StepHandler;

public final class AwaitSqlStepHandler implements StepHandler<AwaitSqlExecutableStep> {
  private final JdbcRuntime jdbc;

  public AwaitSqlStepHandler(JdbcRuntime jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Class<AwaitSqlExecutableStep> stepType() {
    return AwaitSqlExecutableStep.class;
  }

  @Override
  public StepExecutionResult execute(AwaitSqlExecutableStep step, StepExecutionContext context) {
    PollResult<JdbcQueryResult> outcome =
        context
            .services()
            .polling()
            .until(
                request(step.polling()),
                () -> query(step, context),
                result -> {
                  SqlStepHandler.capture(step, result, context);
                  return SqlStepHandler.failure(step.expect(), result, context) == null;
                });
    Map<String, Object> evidence = new LinkedHashMap<>();
    if (outcome.lastValue() != null) evidence.put("lastResult", outcome.lastValue().toEvidence());
    evidence.put("observations", outcome.observations());
    if (outcome.matched()) return StepExecutionResult.success(Map.of(), evidence);
    String message =
        outcome.lastValue() == null
            ? "No database result was observed"
            : SqlStepHandler.failure(step.expect(), outcome.lastValue(), context);
    return StepExecutionResult.failure(message == null ? "Polling timed out" : message, evidence);
  }

  private JdbcQueryResult query(AwaitSqlExecutableStep step, StepExecutionContext context) {
    String sql =
        step.queryFile() == null
            ? SqlStepHandler.resolve(step.query(), context)
            : context.services().resources().readText(step.queryFile());
    return jdbc.execute(
        step.connection(), sql, SqlStepHandler.resolveMap(step.parameters(), context));
  }

  private static PollRequest request(Map<String, Object> polling) {
    Map<String, Object> values = polling == null ? Map.of() : polling;
    Duration timeout = duration(values.getOrDefault("timeout", "30s"));
    Duration interval = duration(values.getOrDefault("interval", "1s"));
    return new PollRequest(timeout, interval);
  }

  private static Duration duration(Object value) {
    return value instanceof Duration duration
        ? duration
        : JdbcDurations.parse(String.valueOf(value));
  }
}
