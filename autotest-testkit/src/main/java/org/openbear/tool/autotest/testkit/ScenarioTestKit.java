package org.openbear.tool.autotest.testkit;

import java.nio.file.Path;
import java.util.Objects;
import org.openbear.tool.autotest.core.config.EnvironmentConfig;
import org.openbear.tool.autotest.core.config.ProjectConfig;
import org.openbear.tool.autotest.core.model.ScenarioPlan;
import org.openbear.tool.autotest.core.plugin.PluginRegistry;
import org.openbear.tool.autotest.core.util.Workspace;
import org.openbear.tool.autotest.dsl.ConfigLoader;
import org.openbear.tool.autotest.dsl.ScenarioCompiler;

/**
 * Small programmatic facade for extension/module tests. It intentionally does not bootstrap
 * production adapters.
 */
public class ScenarioTestKit {
  private final Workspace workspace;
  private final ConfigLoader configs;
  private final ScenarioCompiler scenarios;
  private final PluginRegistry plugins;

  public ScenarioTestKit(Path root) {
    this(root, new PluginRegistry());
  }

  public ScenarioTestKit(Path root, PluginRegistry plugins) {
    this.workspace = new Workspace(root);
    this.configs = new ConfigLoader(workspace);
    this.scenarios = new ScenarioCompiler(workspace);
    this.plugins = Objects.requireNonNull(plugins, "plugins");
  }

  public Workspace workspace() {
    return workspace;
  }

  /** Returns the public-SPI plugin registry supplied for this test fixture. */
  public PluginRegistry plugins() {
    return plugins;
  }

  public ProjectConfig project() {
    return configs.loadProject();
  }

  public EnvironmentConfig environment(String name) {
    return configs.loadEnvironment(name);
  }

  public ScenarioPlan scenario(String idOrPath) {
    return scenarios.find(idOrPath);
  }
}
