package org.openbear.tool.autotest.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openbear.tool.autotest.jdbc.spi.JdbcDriverProvider;
import org.openbear.tool.autotest.spi.resource.DatabaseResource;

class JdbcDriverSelectionTest {
  @Test
  void driverContractsSelectByResourceDriver() {
    JdbcDriverProvider mysql =
        new JdbcDriverProvider() {
          @Override
          public String id() {
            return "mysql";
          }

          @Override
          public boolean supports(DatabaseResource resource) {
            return id().equalsIgnoreCase(resource.driver());
          }

          @Override
          public String validationQuery() {
            return "SELECT 1";
          }
        };
    DatabaseResource resource =
        new DatabaseResource(
            "main",
            "mysql",
            "jdbc:mysql://localhost/db",
            null,
            null,
            2,
            Duration.ofSeconds(1),
            Duration.ofSeconds(1),
            false,
            java.util.Map.of());
    assertEquals(true, List.of(mysql).stream().anyMatch(provider -> provider.supports(resource)));
  }
}
