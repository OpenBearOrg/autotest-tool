package org.openbear.tool.autotest.core.database;

import java.util.Map;

public interface DatabaseAdapter extends AutoCloseable {
  QueryResult execute(String connectionName, String sql, Map<String, Object> params);

  void ping(String connectionName);

  @Override
  void close();
}
