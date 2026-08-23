package org.openbear.tool.autotest.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.openbear.tool.autotest.core.util.DurationParser;

class DurationParserTest {
  @Test
  void parsesHumanAndIsoDurations() {
    assertEquals(Duration.ofSeconds(30), DurationParser.parse("30s"));
    assertEquals(Duration.ofMinutes(5), DurationParser.parse("5m"));
    assertEquals(Duration.ofMinutes(2), DurationParser.parse("PT2M"));
  }

  @Test
  void rejectsInvalidDuration() {
    assertThrows(IllegalArgumentException.class, () -> DurationParser.parse("banana"));
  }
}
