package org.openbear.tool.autotest.spi.service;

public interface ResourceAccess {
  String readText(String workspaceRelativePath);

  byte[] readBytes(String workspaceRelativePath);
}
