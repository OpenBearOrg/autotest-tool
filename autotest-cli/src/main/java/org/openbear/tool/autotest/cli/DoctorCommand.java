package org.openbear.tool.autotest.cli;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import org.openbear.tool.autotest.core.ExitCode;
import org.openbear.tool.autotest.core.domain.CompiledEnvironmentView;
import org.openbear.tool.autotest.core.secret.EnvironmentSecretProvider;
import org.openbear.tool.autotest.core.util.Workspace;
import org.openbear.tool.autotest.dsl.ConfigLoader;
import org.openbear.tool.autotest.dsl.compile.ValidationRequest;
import org.openbear.tool.autotest.dsl.compile.WorkspaceCompiler;
import org.openbear.tool.autotest.spi.plugin.PluginRuntimeContext;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "doctor",
    description = "Check configured HTTP, Oracle, ActiveMQ connections and secrets")
public class DoctorCommand implements Callable<Integer> {
  @Option(names = "--workspace", defaultValue = ".")
  Path workspacePath;

  @Option(names = "--env")
  String envName;

  @Option(names = "--plugin-dir", description = "External plugin directory; repeatable")
  List<Path> pluginDirs = new ArrayList<>();

  @Override
  public Integer call() {
    Workspace w = CliSupport.workspace(workspacePath);
    try (var plugins = PluginBootstrap.dynamicRegistry(resolvePluginDirs(w))) {
      var validation =
          new WorkspaceCompiler(plugins, java.time.Clock.systemUTC())
              .validate(new ValidationRequest(w.root()));
      if (!validation.valid())
        throw new org.openbear.tool.autotest.dsl.ValidationException(
            "Workspace validation failed",
            validation.messages().stream().map(message -> message.message()).toList());
      String environmentName =
          CliSupport.environmentName(envName, new ConfigLoader(w).loadProject());
      var environment =
          validation.environments().stream()
              .filter(value -> environmentName.equalsIgnoreCase(value.name()))
              .findFirst()
              .orElseThrow(
                  () ->
                      new IllegalArgumentException(
                          "Environment is not configured: " + environmentName));
      System.out.println("Autotest environment doctor: " + environment.name());
      plugins.open(
          new PluginRuntimeContext(
              new CompiledEnvironmentView(environment),
              new PublicEngineRuntimeFactory.WorkspaceResources(w),
              new EnvironmentSecretProvider()::resolve,
              java.time.Clock.systemUTC()));
      boolean ok = CliSupport.printPublicDoctor(CliSupport.doctor(plugins));
      return ok ? 0 : ExitCode.ENVIRONMENT_ERROR;
    }
  }

  private List<Path> resolvePluginDirs(Workspace workspace) {
    return pluginDirs.stream()
        .map(path -> path.isAbsolute() ? path : workspace.root().resolve(path))
        .toList();
  }
}
