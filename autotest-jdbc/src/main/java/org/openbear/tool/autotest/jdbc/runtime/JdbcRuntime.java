package org.openbear.tool.autotest.jdbc.runtime;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.openbear.tool.autotest.jdbc.JdbcQueryResult;
import org.openbear.tool.autotest.jdbc.JdbcValueNormalizer;
import org.openbear.tool.autotest.jdbc.NamedParameterSql;
import org.openbear.tool.autotest.jdbc.spi.JdbcDriverProvider;
import org.openbear.tool.autotest.spi.resource.DatabaseResource;
import org.openbear.tool.autotest.spi.resource.EnvironmentView;
import org.openbear.tool.autotest.spi.service.Secrets;

public final class JdbcRuntime implements AutoCloseable {
  private final EnvironmentView environment;
  private final JdbcPoolManager pools;

  public JdbcRuntime(
      EnvironmentView environment, Secrets secrets, List<? extends JdbcDriverProvider> drivers) {
    this.environment = environment;
    this.pools = new JdbcPoolManager(environment, secrets, drivers);
  }

  public JdbcQueryResult execute(
      String connectionName, String sql, Map<String, Object> parameters) {
    DatabaseResource resource = resource(connectionName);
    boolean allowWrites = resource.allowWrites();
    if (!allowWrites && !org.openbear.tool.autotest.jdbc.runtime.SqlPolicy.isReadOnly(sql))
      throw new IllegalStateException(
          "Database writes are disabled by environment databasePolicy.allowWrites=false");
    NamedParameterSql parsed = NamedParameterSql.parse(sql);
    Instant start = Instant.now();
    try (Connection connection = pools.pool(connectionName).getConnection()) {
      if (!allowWrites)
        try {
          connection.setReadOnly(true);
        } catch (SQLException ignored) {
        }
      try (PreparedStatement statement = connection.prepareStatement(parsed.sql())) {
        JdbcParameterBinder.bind(statement, parsed.parameterNames(), parameters);
        boolean hasResult = statement.execute();
        List<Map<String, Object>> rows = hasResult ? read(statement.getResultSet()) : List.of();
        int updateCount = hasResult ? 0 : statement.getUpdateCount();
        return new JdbcQueryResult(rows, updateCount, Duration.between(start, Instant.now()));
      }
    } catch (SQLException e) {
      throw new IllegalStateException(
          "SQL execution failed on connection '" + connectionName + "': " + e.getMessage(), e);
    }
  }

  public void ping(String connectionName) {
    JdbcDriverProvider driver = pools.driver(connectionName);
    execute(connectionName, driver.validationQuery(), Map.of());
  }

  @Override
  public void close() {
    pools.close();
  }

  private DatabaseResource resource(String name) {
    return environment
        .database(name)
        .orElseThrow(
            () -> new IllegalArgumentException("Database connection is not configured: " + name));
  }

  private static List<Map<String, Object>> read(ResultSet resultSet) throws SQLException {
    List<Map<String, Object>> rows = new ArrayList<>();
    ResultSetMetaData metadata = resultSet.getMetaData();
    while (resultSet.next()) {
      Map<String, Object> row = new LinkedHashMap<>();
      for (int i = 1; i <= metadata.getColumnCount(); i++) {
        String label = metadata.getColumnLabel(i);
        row.put(
            label == null ? metadata.getColumnName(i) : label,
            JdbcValueNormalizer.normalizeValue(resultSet.getObject(i)));
      }
      rows.add(row);
    }
    resultSet.close();
    return rows;
  }
}
