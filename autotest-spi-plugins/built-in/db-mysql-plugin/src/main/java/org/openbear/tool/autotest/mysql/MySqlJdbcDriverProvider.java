package org.openbear.tool.autotest.mysql;

import org.openbear.tool.autotest.jdbc.spi.JdbcDriverProvider;
import org.openbear.tool.autotest.spi.resource.DatabaseResource;

public final class MySqlJdbcDriverProvider implements JdbcDriverProvider {
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
}
