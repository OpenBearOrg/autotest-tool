package org.openbear.tool.autotest.core.event;

@FunctionalInterface
public interface ExecutionListener {
  void onEvent(ExecutionEventEnvelope event);
}
