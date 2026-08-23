package org.openbear.tool.autotest.core;

public final class ExitCode {
  public static final int SUCCESS = 0;
  public static final int TEST_FAILURE = 1;
  public static final int CONFIGURATION_ERROR = 2;
  public static final int DSL_VALIDATION_ERROR = 3;
  public static final int ENVIRONMENT_ERROR = 4;
  public static final int INTERNAL_ERROR = 5;

  private ExitCode() {}
}
