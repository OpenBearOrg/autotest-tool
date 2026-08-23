package org.openbear.tool.autotest.http;

import org.openbear.tool.autotest.spi.step.StepCompileContext;
import org.openbear.tool.autotest.spi.step.StepTypeProvider;

final class HttpStepTypeProvider
    implements StepTypeProvider<HttpConfiguration, HttpExecutableStep> {
  @Override
  public String type() {
    return "http";
  }

  @Override
  public Class<HttpConfiguration> configurationType() {
    return HttpConfiguration.class;
  }

  @Override
  public String schemaResource() {
    return "";
  }

  @Override
  public HttpExecutableStep compile(HttpConfiguration configuration, StepCompileContext context) {
    return new HttpExecutableStep(
        configuration.identity(),
        configuration.service(),
        configuration.request(),
        configuration.expect(),
        configuration.capture());
  }

  @Override
  public Class<HttpExecutableStep> executableType() {
    return HttpExecutableStep.class;
  }
}
