package org.openbear.tool.autotest.core.engine;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.random.RandomGenerator;

/** Evaluates reserved runtime expressions in the {@code random:} namespace. */
public final class RuntimeExpressionResolver {
  private static final String ALPHA_NUMERIC =
      "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
  private static final String NANO_ID = ALPHA_NUMERIC + "-_";
  private static final List<String> COLORS = List.of("red", "fuchsia", "grey", "blue", "green");
  private static final List<String> ABBREVIATIONS = List.of("SQL", "PCI", "JSON", "HTTP", "API");
  private static final List<String> WORDS =
      List.of("protocol", "interface", "quick", "brown", "fox", "order", "customer");

  private final RandomGenerator random;
  private final Clock clock;

  public RuntimeExpressionResolver(RandomGenerator random, Clock clock) {
    this.random = random;
    this.clock = clock;
  }

  public static RuntimeExpressionResolver standard() {
    return new RuntimeExpressionResolver(RandomGenerator.getDefault(), Clock.systemUTC());
  }

  public static boolean isReserved(String token) {
    return token != null && token.startsWith("random:");
  }

  public static void validate(String token) {
    switch (token) {
      case "random:uuid",
          "random:timestamp",
          "random:isoTimestamp",
          "random:nanoId",
          "random:alphaNumeric",
          "random:boolean",
          "random:int",
          "random:color",
          "random:hexColor",
          "random:abbreviation",
          "random:word",
          "random:words" -> {}
      default ->
          throw new IllegalArgumentException("Unsupported runtime expression: ${" + token + "}");
    }
  }

  public Object evaluate(String token) {
    validate(token);
    return switch (token) {
      case "random:uuid" -> java.util.UUID.randomUUID().toString();
      case "random:timestamp" -> Instant.now(clock).getEpochSecond();
      case "random:isoTimestamp" -> Instant.now(clock).toString();
      case "random:nanoId" -> randomText(NANO_ID, 21);
      case "random:alphaNumeric" -> randomText(ALPHA_NUMERIC, 1);
      case "random:boolean" -> random.nextBoolean();
      case "random:int" -> random.nextInt(1_001);
      case "random:color" -> sample(COLORS);
      case "random:hexColor" -> String.format("#%06x", random.nextInt(0x1_000_000));
      case "random:abbreviation" -> sample(ABBREVIATIONS);
      case "random:word" -> sample(WORDS);
      case "random:words" -> String.join(" ", sample(WORDS), sample(WORDS), sample(WORDS));
      default ->
          throw new IllegalStateException(
              "Validated runtime expression is not implemented: " + token);
    };
  }

  private String sample(List<String> values) {
    return values.get(random.nextInt(values.size()));
  }

  private String randomText(String alphabet, int length) {
    StringBuilder value = new StringBuilder(length);
    for (int i = 0; i < length; i++)
      value.append(alphabet.charAt(random.nextInt(alphabet.length())));
    return value.toString();
  }
}
