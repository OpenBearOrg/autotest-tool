package org.openbear.tool.autotest.cli;

import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import org.openbear.tool.autotest.core.util.Workspace;
import org.openbear.tool.autotest.dsl.ValidationException;
import org.openbear.tool.autotest.dsl.compile.ValidationRequest;
import org.openbear.tool.autotest.dsl.compile.ValidationResult;
import org.openbear.tool.autotest.dsl.compile.WorkspaceCompiler;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "validate",
    description =
        "Validate project, environments, suites, scenarios, resources, variables, and DSL")
public class ValidateCommand implements Callable<Integer> {
  @Option(names = "--workspace", defaultValue = ".")
  Path workspacePath;

  @Option(names = "--plugin-dir", description = "External plugin directory; repeatable")
  List<Path> pluginDirs = new ArrayList<>();

  @Override
  public Integer call() throws Exception {
    Workspace w = CliSupport.workspace(workspacePath);
    try (var plugins = PluginBootstrap.dynamicRegistry(resolvePluginDirs(w))) {
      ValidationResult result =
          new WorkspaceCompiler(plugins, Clock.systemUTC())
              .validate(new ValidationRequest(w.root()));
      if (!result.valid()) throw validationFailure(result);
      System.out.printf(
          "VALID: %d scenarios, %d suites, %d environments%n",
          result.scenarios().size(), result.suiteCount(), result.environments().size());
    }
    return 0;
  }

  private ValidationException validationFailure(ValidationResult result) {
    List<String> errors =
        result.messages().stream()
            .filter(message -> message.isError())
            .map(
                message ->
                    message.location().isBlank()
                        ? message.message()
                        : message.location() + ": " + message.message())
            .toList();
    return new ValidationException("Workspace validation failed", errors);
  }

  private List<Path> resolvePluginDirs(Workspace workspace) {
    return pluginDirs.stream()
        .map(path -> path.isAbsolute() ? path : workspace.root().resolve(path))
        .toList();
  }
}
