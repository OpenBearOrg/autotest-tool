package org.openbear.tool.autotest.spi.step;

/** Executes immutable steps; implementations must be safe for concurrent scenarios. @since 1.0 */
public interface StepHandler<S extends ExecutableStep> {
  Class<S> stepType();

  StepExecutionResult execute(S step, StepExecutionContext context) throws Exception;
}
