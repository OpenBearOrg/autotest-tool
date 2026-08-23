package org.openbear.tool.autotest.jdbc.spi;

import com.zaxxer.hikari.HikariConfig;
import org.openbear.tool.autotest.spi.resource.DatabaseResource;
import org.openbear.tool.autotest.spi.service.Secrets;

public interface JdbcDriverProvider {
  String id();

  boolean supports(DatabaseResource resource);

  String validationQuery();

  default void configure(HikariConfig config, DatabaseResource resource, Secrets secrets) {
    config.setJdbcUrl(resource.jdbcUrl());
    if (resource.username() != null) config.setUsername(secrets.require(resource.username()));
    if (resource.password() != null) config.setPassword(secrets.require(resource.password()));
  }
}
