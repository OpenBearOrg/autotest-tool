package org.openbear.tool.autotest.spi.service;

import java.util.Map;

public interface Assertions {
  void verify(Object actual, Object expected);

  void verifyValues(Object document, Map<String, ?> expectations);
}
