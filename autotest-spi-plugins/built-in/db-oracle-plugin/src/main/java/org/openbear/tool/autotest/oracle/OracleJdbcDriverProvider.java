package org.openbear.tool.autotest.oracle;

import com.zaxxer.hikari.HikariConfig;
import org.openbear.tool.autotest.jdbc.runtime.JdbcDriverUnavailableException;
import org.openbear.tool.autotest.jdbc.spi.JdbcDriverProvider;
import org.openbear.tool.autotest.spi.resource.DatabaseResource;
import org.openbear.tool.autotest.spi.service.Secrets;

public final class OracleJdbcDriverProvider implements JdbcDriverProvider {
  private static final String ORACLE_DRIVER_CLASS = "oracle.jdbc.OracleDriver";

  @Override
  public String id() {
    return "oracle";
  }

  @Override
  public boolean supports(DatabaseResource resource) {
    return id().equalsIgnoreCase(resource.driver());
  }

  @Override
  public String validationQuery() {
    return "SELECT 1 AS OK FROM DUAL";
  }

  @Override
  public void configure(HikariConfig config, DatabaseResource resource, Secrets secrets) {
    requireOracleDriver(OracleJdbcDriverProvider.class.getClassLoader());
    config.setDriverClassName(ORACLE_DRIVER_CLASS);
    JdbcDriverProvider.super.configure(config, resource, secrets);
  }

  static void requireOracleDriver(ClassLoader classLoader) {
    try {
      Class.forName(ORACLE_DRIVER_CLASS, true, classLoader);
    } catch (ClassNotFoundException e) {
      throw new JdbcDriverUnavailableException(
          "Oracle JDBC driver not found. Place a compatible user-supplied driver at "
              + "lib/jdbc/ojdbc.jar and start Autotest using bin/autotest-tool.",
          e);
    }
  }
}
