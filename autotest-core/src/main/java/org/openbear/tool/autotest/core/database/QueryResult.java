package org.openbear.tool.autotest.core.database;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QueryResult {
  private int rowCount;
  private Integer updateCount;
  private List<Map<String, Object>> rows = new ArrayList<>();
  private long durationMs;

  public void setRows(List<Map<String, Object>> rows) {
    try {
      this.rows = JdbcValueNormalizer.normalizeRows(rows);
    } catch (SQLException e) {
      throw new IllegalStateException("Failed to normalize SQL result rows", e);
    }
  }

  public Map<String, Object> toMap() {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("rowCount", rowCount);
    m.put("updateCount", updateCount);
    m.put("rows", rows);
    m.put("durationMs", durationMs);
    return m;
  }
}
