package org.openbear.tool.autotest.jdbc.step;

import org.openbear.tool.autotest.spi.step.StepCompileContext;
import org.openbear.tool.autotest.spi.step.StepTypeProvider;

public final class SqlStepTypeProvider
    implements StepTypeProvider<SqlConfiguration, SqlExecutableStep> {
  @Override
  public String type() {
    return "sql";
  }

  @Override
  public Class<SqlConfiguration> configurationType() {
    return SqlConfiguration.class;
  }

  @Override
  public String schemaResource() {
    return "";
  }

  @Override
  public SqlExecutableStep compile(SqlConfiguration configuration, StepCompileContext context) {
    return new SqlExecutableStep(
        configuration.identity(),
        configuration.connection(),
        configuration.queryFile(),
        configuration.query(),
        configuration.parameters(),
        configuration.expect(),
        configuration.capture());
  }

  @Override
  public Class<SqlExecutableStep> executableType() {
    return SqlExecutableStep.class;
  }
}
