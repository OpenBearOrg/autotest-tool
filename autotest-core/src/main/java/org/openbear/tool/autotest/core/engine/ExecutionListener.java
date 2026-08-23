package org.openbear.tool.autotest.core.engine;

import org.openbear.tool.autotest.core.event.ExecutionEventEnvelope;

/** Compatibility alias for the Group 6 listener contract. */
@Deprecated(forRemoval = false)
public interface ExecutionListener extends org.openbear.tool.autotest.core.event.ExecutionListener {
  @Override
  default void onEvent(ExecutionEventEnvelope event) {}
}
