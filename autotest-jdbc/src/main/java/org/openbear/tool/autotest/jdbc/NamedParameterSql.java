package org.openbear.tool.autotest.jdbc;

import java.util.ArrayList;
import java.util.List;

public record NamedParameterSql(String sql, List<String> parameterNames) {
  public static NamedParameterSql parse(String input) {
    StringBuilder out = new StringBuilder();
    List<String> names = new ArrayList<>();
    State state = State.DEFAULT;
    for (int i = 0; i < input.length(); i++) {
      char c = input.charAt(i);
      char n = i + 1 < input.length() ? input.charAt(i + 1) : '\0';
      switch (state) {
        case DEFAULT -> {
          if (c == '\'') {
            state = State.SINGLE;
            out.append(c);
          } else if (c == '"') {
            state = State.DOUBLE;
            out.append(c);
          } else if (c == '-' && n == '-') {
            state = State.LINE_COMMENT;
            out.append(c).append(n);
            i++;
          } else if (c == '/' && n == '*') {
            state = State.BLOCK_COMMENT;
            out.append(c).append(n);
            i++;
          } else if (c == ':' && n != '=' && n != ':' && Character.isJavaIdentifierStart(n)) {
            int j = i + 1;
            while (j < input.length() && Character.isJavaIdentifierPart(input.charAt(j))) j++;
            names.add(input.substring(i + 1, j));
            out.append('?');
            i = j - 1;
          } else out.append(c);
        }
        case SINGLE -> {
          out.append(c);
          if (c == '\'') {
            if (n == '\'') {
              out.append(n);
              i++;
            } else state = State.DEFAULT;
          }
        }
        case DOUBLE -> {
          out.append(c);
          if (c == '"') state = State.DEFAULT;
        }
        case LINE_COMMENT -> {
          out.append(c);
          if (c == '\n') state = State.DEFAULT;
        }
        case BLOCK_COMMENT -> {
          out.append(c);
          if (c == '*' && n == '/') {
            out.append(n);
            i++;
            state = State.DEFAULT;
          }
        }
      }
    }
    return new NamedParameterSql(out.toString(), List.copyOf(names));
  }

  enum State {
    DEFAULT,
    SINGLE,
    DOUBLE,
    LINE_COMMENT,
    BLOCK_COMMENT
  }
}
