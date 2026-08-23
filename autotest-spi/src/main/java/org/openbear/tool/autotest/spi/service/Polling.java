package org.openbear.tool.autotest.spi.service;

import java.util.function.Predicate;
import java.util.function.Supplier;

@FunctionalInterface
public interface Polling {
  <T> PollResult<T> until(PollRequest request, Supplier<T> probe, Predicate<T> done);
}
