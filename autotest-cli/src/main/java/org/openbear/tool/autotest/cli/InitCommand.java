package org.openbear.tool.autotest.cli;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "init", description = "Create an empty Autotest workspace")
public class InitCommand implements Callable<Integer> {
  @Option(names = "--workspace", defaultValue = ".")
  Path root;

  @Option(names = "--force")
  boolean force;

  @Override
  public Integer call() throws Exception {
    root = root.toAbsolutePath().normalize();
    Files.createDirectories(root);
    write(
        "autotest-tool.yaml",
        "projectVersion: \"1.0\"\nname: Autotest Workspace\ndefaults:\n  environment: sit\n  polling:\n    timeout: 2m\n    interval: "
            + "3s\nreporting:\n  directory: reports\n  payloadMaxSize: 1MB\nexecution:\n  parallelism: 1\n  failFast: false\n");
    Files.createDirectories(root.resolve("environments"));
    Files.createDirectories(root.resolve("suites"));
    Files.createDirectories(root.resolve("scenarios"));
    Files.createDirectories(root.resolve("payloads"));
    Files.createDirectories(root.resolve("sql"));
    System.out.println("Initialized Autotest workspace: " + root);
    return 0;
  }

  private void write(String rel, String content) throws Exception {
    Path p = root.resolve(rel);
    if (Files.exists(p) && !force) return;
    Files.createDirectories(p.getParent());
    Files.writeString(p, content, StandardCharsets.UTF_8);
  }
}
