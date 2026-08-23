package org.openbear.tool.autotest.jdbc.runtime;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.openbear.tool.autotest.spi.resource.DatabaseResource;
import org.openbear.tool.autotest.spi.resource.EnvironmentView;

public final class MapEnvironmentView implements EnvironmentView {
  private final String name;
  private final Map<String, DatabaseResource> databases;

  public MapEnvironmentView(String name, Map<String, DatabaseResource> databases) {
    this.name = name;
    this.databases = Map.copyOf(new LinkedHashMap<>(databases));
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public Optional<DatabaseResource> database(String resourceName) {
    return Optional.ofNullable(databases.get(resourceName));
  }

  @Override
  public List<DatabaseResource> databases() {
    return List.copyOf(databases.values());
  }
}
