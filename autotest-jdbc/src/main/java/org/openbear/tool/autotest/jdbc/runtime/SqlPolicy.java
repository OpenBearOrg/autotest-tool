package org.openbear.tool.autotest.jdbc.runtime;

import java.util.Locale;

public final class SqlPolicy {
  private SqlPolicy() {}

  public static boolean isReadOnly(String sql) {
    String normalized =
        stripLeadingComments(sql == null ? "" : sql).stripLeading().toLowerCase(Locale.ROOT);
    return normalized.startsWith("select")
        || normalized.startsWith("with")
        || normalized.startsWith("show")
        || normalized.startsWith("describe")
        || normalized.startsWith("explain");
  }

  private static String stripLeadingComments(String value) {
    String current = value;
    boolean changed = true;
    while (changed) {
      changed = false;
      current = current.stripLeading();
      if (current.startsWith("--")) {
        int end = current.indexOf('\n');
        current = end < 0 ? "" : current.substring(end + 1);
        changed = true;
      } else if (current.startsWith("/*")) {
        int end = current.indexOf("*/", 2);
        if (end >= 0) {
          current = current.substring(end + 2);
          changed = true;
        }
      }
    }
    return current;
  }
}
