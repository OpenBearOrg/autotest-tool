package org.openbear.tool.autotest.jdbc.step;

import org.openbear.tool.autotest.spi.step.StepCompileContext;
import org.openbear.tool.autotest.spi.step.StepTypeProvider;

public final class AwaitSqlStepTypeProvider
    implements StepTypeProvider<AwaitSqlConfiguration, AwaitSqlExecutableStep> {
  @Override
  public String type() {
    return "awaitSql";
  }

  @Override
  public Class<AwaitSqlConfiguration> configurationType() {
    return AwaitSqlConfiguration.class;
  }

  @Override
  public String schemaResource() {
    return "";
  }

  @Override
  public AwaitSqlExecutableStep compile(
      AwaitSqlConfiguration configuration, StepCompileContext context) {
    return new AwaitSqlExecutableStep(
        configuration.identity(),
        configuration.connection(),
        configuration.queryFile(),
        configuration.query(),
        configuration.parameters(),
        configuration.expect(),
        configuration.capture(),
        configuration.polling());
  }

  @Override
  public Class<AwaitSqlExecutableStep> executableType() {
    return AwaitSqlExecutableStep.class;
  }
}
