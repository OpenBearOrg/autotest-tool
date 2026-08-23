package org.openbear.tool.autotest.core.util;

import java.time.Duration;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DurationParser {
  private static final Pattern SIMPLE =
      Pattern.compile("^([0-9]+)(ms|s|m|h)$", Pattern.CASE_INSENSITIVE);

  private DurationParser() {}

  public static Duration parse(String value) {
    if (value == null || value.isBlank())
      throw new IllegalArgumentException("Duration must not be blank");
    String v = value.trim();
    if (v.toUpperCase(Locale.ROOT).startsWith("P"))
      return Duration.parse(v.toUpperCase(Locale.ROOT));
    Matcher m = SIMPLE.matcher(v);
    if (!m.matches())
      throw new IllegalArgumentException(
          "Invalid duration '" + value + "'. Use 500ms, 30s, 2m, 1h, or ISO-8601 such as PT2M");
    long n = Long.parseLong(m.group(1));
    return switch (m.group(2).toLowerCase(Locale.ROOT)) {
      case "ms" -> Duration.ofMillis(n);
      case "s" -> Duration.ofSeconds(n);
      case "m" -> Duration.ofMinutes(n);
      case "h" -> Duration.ofHours(n);
      default -> throw new IllegalStateException();
    };
  }
}
