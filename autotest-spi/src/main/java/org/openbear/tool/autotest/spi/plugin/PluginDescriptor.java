package org.openbear.tool.autotest.spi.plugin;

import java.util.Objects;

/** Immutable plugin identity and SPI compatibility declaration. @since 1.0 */
public record PluginDescriptor(String id, String name, String version, String spiVersion) {
  public PluginDescriptor {
    id = requireText(id, "id");
    name = requireText(name, "name");
    version = requireText(version, "version");
    spiVersion = requireText(spiVersion, "spiVersion");
  }

  private static String requireText(String value, String name) {
    String trimmed = Objects.requireNonNull(value, name).trim();
    if (trimmed.isEmpty()) throw new IllegalArgumentException(name + " must not be blank");
    return trimmed;
  }
}
