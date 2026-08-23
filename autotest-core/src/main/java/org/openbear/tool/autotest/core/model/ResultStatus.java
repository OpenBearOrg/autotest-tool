package org.openbear.tool.autotest.core.model;

public enum ResultStatus {
  PASS,
  FAIL,
  SKIPPED,
  ERROR;

  public boolean isFailure() {
    return this == FAIL || this == ERROR;
  }
}
