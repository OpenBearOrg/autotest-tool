package org.openbear.tool.autotest.spi.plugin;

import java.util.Collection;
import java.util.List;
import org.openbear.tool.autotest.spi.doctor.DoctorCheck;
import org.openbear.tool.autotest.spi.step.StepHandler;

/** Runtime-owned resources and handlers for one opened plugin registry. @since 1.0 */
public interface PluginRuntime extends AutoCloseable {
  default Collection<? extends StepHandler<?>> stepHandlers() {
    return List.of();
  }

  default Collection<? extends DoctorCheck> doctorChecks() {
    return List.of();
  }

  @Override
  default void close() {}

  static PluginRuntime empty() {
    return new PluginRuntime() {};
  }
}
