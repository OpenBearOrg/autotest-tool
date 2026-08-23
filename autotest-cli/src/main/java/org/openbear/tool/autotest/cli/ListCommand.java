package org.openbear.tool.autotest.cli;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import org.openbear.tool.autotest.core.plugin.PluginRegistry;
import org.openbear.tool.autotest.core.util.Workspace;
import org.openbear.tool.autotest.dsl.ScenarioCompiler;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "list", description = "List scenarios, suites, plugins or step types")
public class ListCommand implements Callable<Integer> {
  @Option(names = "--workspace", defaultValue = ".")
  Path workspacePath;

  @Parameters(index = "0", arity = "0..1", description = "plugins or step-types")
  String category;

  @Override
  public Integer call() throws Exception {
    if ("plugins".equalsIgnoreCase(category)) return listPlugins();
    if ("step-types".equalsIgnoreCase(category)) return listStepTypes();
    Workspace w = CliSupport.workspace(workspacePath);
    System.out.println("Scenarios:");
    for (var p : new ScenarioCompiler(w).discover())
      System.out.printf(
          "  %-30s %s  %s%n", p.scenario().getId(), p.scenario().getName(), p.scenario().getTags());
    System.out.println("Suites:");
    Path root = w.resolve("suites");
    if (Files.exists(root))
      try (var s = Files.walk(root)) {
        s.filter(Files::isRegularFile)
            .filter(p -> p.toString().endsWith(".yaml") || p.toString().endsWith(".yml"))
            .forEach(p -> System.out.println("  " + w.root().relativize(p)));
      }
    return 0;
  }

  private int listPlugins() {
    try (PluginRegistry registry = PluginBootstrap.dynamicRegistry()) {
      System.out.println("PLUGIN        VERSION   SPI   CONTRIBUTIONS");
      for (var plugin : registry.plugins()) {
        String steps =
            plugin.stepTypes().stream()
                .map(provider -> provider.type())
                .sorted()
                .toList()
                .toString();
        System.out.printf(
            "%-13s %-9s %-5s %s%n",
            plugin.descriptor().id(),
            plugin.descriptor().version(),
            plugin.descriptor().spiVersion(),
            steps);
      }
    }
    return 0;
  }

  private int listStepTypes() {
    try (PluginRegistry registry = PluginBootstrap.dynamicRegistry()) {
      System.out.println("TYPE             PLUGIN");
      for (var plugin : registry.plugins())
        for (var provider : plugin.stepTypes())
          System.out.printf("%-16s %s%n", provider.type(), plugin.descriptor().id());
    }
    return 0;
  }
}
