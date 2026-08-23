package org.openbear.tool.autotest.core.domain;

import java.time.Instant;
import java.util.Objects;

public record CompilationMetadata(String dslVersion, String gitCommit, Instant compiledAt) {
  public CompilationMetadata {
    dslVersion = IdValue.requireNonBlank(dslVersion, "dslVersion");
    compiledAt = Objects.requireNonNull(compiledAt, "compiledAt");
  }
}
