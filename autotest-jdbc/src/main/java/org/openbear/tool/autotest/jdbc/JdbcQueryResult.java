package org.openbear.tool.autotest.jdbc;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record JdbcQueryResult(List<Map<String, Object>> rows, int updateCount, Duration duration) {
  public JdbcQueryResult {
    rows = rows == null ? List.of() : rows.stream().map(row -> Map.copyOf(row)).toList();
    duration = Objects.requireNonNull(duration, "duration");
  }

  public int rowCount() {
    return rows.size();
  }

  public Map<String, Object> toEvidence() {
    return Map.of(
        "rows",
        rows,
        "rowCount",
        rowCount(),
        "updateCount",
        updateCount,
        "durationMs",
        duration.toMillis());
  }
}
