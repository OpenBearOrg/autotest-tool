package org.openbear.tool.autotest.cli;

import java.util.concurrent.Callable;
import org.openbear.tool.autotest.core.AutotestVersion;
import picocli.CommandLine.Command;

@Command(name = "version", description = "Print tool and DSL versions")
public class VersionCommand implements Callable<Integer> {
  @Override
  public Integer call() {
    System.out.println(
        "autotest-tool " + AutotestVersion.VERSION + " (DSL " + AutotestVersion.DSL_VERSION + ")");
    return 0;
  }
}
