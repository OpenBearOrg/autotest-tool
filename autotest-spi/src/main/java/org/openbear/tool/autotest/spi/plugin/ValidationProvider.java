package org.openbear.tool.autotest.spi.plugin;

import java.util.List;
import org.openbear.tool.autotest.spi.resource.EnvironmentView;
import org.openbear.tool.autotest.spi.step.ExecutableStep;

/**
 * Performs plugin-specific semantic validation without executing external operations. @since 1.0
 */
@FunctionalInterface
public interface ValidationProvider {
  void validate(ValidationContext context, ValidationReporter reporter);

  record ValidationContext(EnvironmentView environment, List<? extends ExecutableStep> steps) {}
}
