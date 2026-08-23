package org.openbear.tool.autotest.core.scheduler;

import java.util.Collection;
import java.util.List;
import org.openbear.tool.autotest.core.domain.CompiledScenario;

@FunctionalInterface
public interface ScenarioPolicyProvider<T> {
  boolean parallelSafe(T scenario);

  default Collection<ResourceClaim> claims(T scenario) {
    return List.of();
  }

  static <T> ScenarioPolicyProvider<T> defaults() {
    return scenario -> {
      if (scenario instanceof CompiledScenario compiled)
        return compiled.executionPolicy().isParallelSafe();
      return true;
    };
  }
}
