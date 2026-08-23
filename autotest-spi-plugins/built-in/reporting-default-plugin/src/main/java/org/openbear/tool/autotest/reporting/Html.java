package org.openbear.tool.autotest.reporting;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

final class Html {
  private static final String CSS = resource("/report.css");
  private static final String JAVASCRIPT = resource("/report.js");

  private Html() {}

  static String esc(Object value) {
    if (value == null) return "";
    return String.valueOf(value)
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;");
  }

  static String page(String title, String body) {
    return "<!doctype html><html><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><title>"
        + esc(title)
        + "</title><style>"
        + CSS
        + "</style></head><body>"
        + body
        + "<script>"
        + JAVASCRIPT
        + "</script></body></html>";
  }

  private static String resource(String name) {
    try (InputStream in = Html.class.getResourceAsStream(name)) {
      if (in == null) throw new IllegalStateException("Missing reporting resource: " + name);
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to load reporting resource: " + name, e);
    }
  }
}
