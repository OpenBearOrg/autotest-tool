package org.openbear.tool.autotest.core.util;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ByteSizeParser {
  private static final Pattern P =
      Pattern.compile("^([0-9]+)\\s*(B|KB|MB|GB)?$", Pattern.CASE_INSENSITIVE);

  private ByteSizeParser() {}

  public static long parse(String value) {
    if (value == null || value.isBlank()) return 1024L * 1024L;
    Matcher m = P.matcher(value.trim());
    if (!m.matches()) throw new IllegalArgumentException("Invalid byte size: " + value);
    long n = Long.parseLong(m.group(1));
    String u = m.group(2) == null ? "B" : m.group(2).toUpperCase(Locale.ROOT);
    return switch (u) {
      case "B" -> n;
      case "KB" -> n * 1024L;
      case "MB" -> n * 1024L * 1024L;
      case "GB" -> n * 1024L * 1024L * 1024L;
      default -> n;
    };
  }
}
