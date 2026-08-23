package org.openbear.tool.autotest.spi.service;

import java.util.Optional;

@FunctionalInterface
public interface Secrets {
  Optional<String> resolve(String reference);

  default String require(String reference) {
    return resolve(reference)
        .orElseThrow(() -> new IllegalArgumentException("Secret is not available: " + reference));
  }
}
