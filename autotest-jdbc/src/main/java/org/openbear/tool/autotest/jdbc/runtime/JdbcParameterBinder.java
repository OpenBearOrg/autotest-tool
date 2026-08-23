package org.openbear.tool.autotest.jdbc.runtime;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

final class JdbcParameterBinder {
  private JdbcParameterBinder() {}

  static void bind(PreparedStatement statement, List<String> names, Map<String, Object> parameters)
      throws SQLException {
    for (int index = 0; index < names.size(); index++) {
      String name = names.get(index);
      if (!parameters.containsKey(name))
        throw new IllegalArgumentException("SQL parameter is not provided: " + name);
      statement.setObject(index + 1, parameters.get(name));
    }
  }
}
