package org.openbear.tool.autotest.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class NamedParameterSqlTest {
  @Test
  void ignoresQuotedAndCommentedColons() {
    NamedParameterSql parsed =
        NamedParameterSql.parse("select ':ignored', value from t where id=:id -- :comment\n");
    assertEquals("select ':ignored', value from t where id=? -- :comment\n", parsed.sql());
    assertEquals(List.of("id"), parsed.parameterNames());
  }
}
