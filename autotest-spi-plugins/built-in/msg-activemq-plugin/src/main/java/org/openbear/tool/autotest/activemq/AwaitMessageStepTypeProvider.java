package org.openbear.tool.autotest.activemq;

import org.openbear.tool.autotest.spi.step.StepCompileContext;
import org.openbear.tool.autotest.spi.step.StepTypeProvider;

final class AwaitMessageStepTypeProvider
    implements StepTypeProvider<AwaitMessageConfiguration, AwaitMessageExecutableStep> {
  @Override
  public String type() {
    return "awaitMessage";
  }

  @Override
  public Class<AwaitMessageConfiguration> configurationType() {
    return AwaitMessageConfiguration.class;
  }

  @Override
  public String schemaResource() {
    return "";
  }

  @Override
  public AwaitMessageExecutableStep compile(
      AwaitMessageConfiguration configuration, StepCompileContext context) {
    return new AwaitMessageExecutableStep(
        configuration.identity(),
        configuration.connection(),
        configuration.destination(),
        configuration.observationMode(),
        configuration.selector(),
        configuration.polling(),
        configuration.match(),
        configuration.expect(),
        configuration.capture());
  }

  @Override
  public Class<AwaitMessageExecutableStep> executableType() {
    return AwaitMessageExecutableStep.class;
  }
}
