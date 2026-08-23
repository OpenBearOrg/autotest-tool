package org.openbear.tool.autotest.spi.plugin;

public interface ValidationReporter {
  void error(String path, String message);

  void warning(String path, String message);
}
