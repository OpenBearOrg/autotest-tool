package org.openbear.tool.autotest.oracle;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.openbear.tool.autotest.jdbc.runtime.JdbcDriverUnavailableException;

class OracleJdbcDriverProviderTest {
  @Test
  void reportsActionableErrorWhenOracleDriverIsUnavailable() {
    JdbcDriverUnavailableException failure =
        assertThrows(
            JdbcDriverUnavailableException.class,
            () ->
                OracleJdbcDriverProvider.requireOracleDriver(ClassLoader.getPlatformClassLoader()));

    assertTrue(failure.getMessage().contains("Oracle JDBC driver not found"));
    assertTrue(failure.getMessage().contains("lib/jdbc/ojdbc.jar"));
    assertTrue(failure.getMessage().contains("bin/autotest-tool"));
  }
}
