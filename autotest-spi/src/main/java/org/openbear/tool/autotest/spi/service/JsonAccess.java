package org.openbear.tool.autotest.spi.service;

import java.util.Optional;

public interface JsonAccess {
  Optional<Object> read(Object document, String path);

  Object require(Object document, String path);

  Object parse(String json);

  String write(Object value);
}
