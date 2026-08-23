package org.openbear.tool.autotest.core.database;

import java.util.Locale;

public final class SqlPolicy {
  private SqlPolicy() {}

  public static boolean isReadOnly(String sql) {
    String s = stripLeadingComments(sql).stripLeading().toLowerCase(Locale.ROOT);
    return s.startsWith("select ")
        || s.startsWith("select\n")
        || s.startsWith("select\t")
        || s.startsWith("with ")
        || s.startsWith("with\n")
        || s.startsWith("with\t");
  }

  private static String stripLeadingComments(String s) {
    String x = s;
    boolean changed = true;
    while (changed) {
      changed = false;
      x = x.stripLeading();
      if (x.startsWith("--")) {
        int n = x.indexOf('\n');
        x = n < 0 ? "" : x.substring(n + 1);
        changed = true;
      } else if (x.startsWith("/*")) {
        int n = x.indexOf("*/", 2);
        if (n >= 0) {
          x = x.substring(n + 2);
          changed = true;
        }
      }
    }
    return x;
  }
}
