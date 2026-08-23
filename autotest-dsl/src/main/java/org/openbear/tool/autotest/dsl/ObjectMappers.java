package org.openbear.tool.autotest.dsl;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public final class ObjectMappers {
  private ObjectMappers() {}

  public static ObjectMapper yaml() {
    return new ObjectMapper(new YAMLFactory())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
        .findAndRegisterModules();
  }

  public static ObjectMapper json() {
    return new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
        .findAndRegisterModules();
  }
}
