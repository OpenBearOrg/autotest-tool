package org.openbear.tool.autotest.dsl.compile;

import java.nio.file.Path;

@FunctionalInterface
public interface GitCommitProvider {
  String commit(Path workspace);
}
