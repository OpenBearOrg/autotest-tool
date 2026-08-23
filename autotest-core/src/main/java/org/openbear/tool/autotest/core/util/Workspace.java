package org.openbear.tool.autotest.core.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class Workspace {
  private final Path root;

  public Workspace(Path root) {
    this.root = root.toAbsolutePath().normalize();
  }

  public Path root() {
    return root;
  }

  public Path resolve(String relative) {
    if (relative == null || relative.isBlank())
      throw new IllegalArgumentException("Resource path must not be blank");
    Path p = root.resolve(relative).normalize();
    if (!p.startsWith(root))
      throw new IllegalArgumentException("Resource escapes workspace: " + relative);
    return p;
  }

  public String readText(String relative) throws IOException {
    return Files.readString(resolve(relative), StandardCharsets.UTF_8);
  }

  public boolean exists(String relative) {
    return Files.exists(resolve(relative));
  }
}
