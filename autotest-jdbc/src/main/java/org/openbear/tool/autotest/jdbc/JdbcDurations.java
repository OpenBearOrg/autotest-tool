package org.openbear.tool.autotest.jdbc;

import java.time.Duration;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class JdbcDurations {
  private static final Pattern SIMPLE =
      Pattern.compile("^([0-9]+)(ms|s|m|h)$", Pattern.CASE_INSENSITIVE);

  private JdbcDurations() {}

  public static Duration parse(String value) {
    if (value == null || value.isBlank())
      throw new IllegalArgumentException("Duration must not be blank");
    String text = value.trim();
    if (text.toUpperCase(Locale.ROOT).startsWith("P"))
      return Duration.parse(text.toUpperCase(Locale.ROOT));
    Matcher matcher = SIMPLE.matcher(text);
    if (!matcher.matches()) throw new IllegalArgumentException("Invalid duration: " + value);
    long amount = Long.parseLong(matcher.group(1));
    return switch (matcher.group(2).toLowerCase(Locale.ROOT)) {
      case "ms" -> Duration.ofMillis(amount);
      case "s" -> Duration.ofSeconds(amount);
      case "m" -> Duration.ofMinutes(amount);
      case "h" -> Duration.ofHours(amount);
      default -> throw new IllegalStateException();
    };
  }
}
