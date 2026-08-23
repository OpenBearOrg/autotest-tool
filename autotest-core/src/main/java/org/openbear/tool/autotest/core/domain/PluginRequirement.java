package org.openbear.tool.autotest.core.domain;

public record PluginRequirement(String capability) {
  public PluginRequirement {
    capability = IdValue.requireNonBlank(capability, "capability");
  }
}
