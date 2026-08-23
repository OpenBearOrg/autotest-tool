package org.openbear.tool.autotest.core.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.openbear.tool.autotest.core.config.RetryConfig;

@Getter
@Setter
public class HttpRequestDefinition {
  private String method = "GET";
  private String path = "/";
  private Map<String, Object> query = new LinkedHashMap<>();
  private Map<String, String> headers = new LinkedHashMap<>();
  private String bodyFile;
  private Object body;
  private List<JsonPatchOperation> patch = new ArrayList<>();
  private String timeout;
  private RetryConfig retry;

  public void setQuery(Map<String, Object> query) {
    this.query = query == null ? new LinkedHashMap<>() : query;
  }

  public void setHeaders(Map<String, String> headers) {
    this.headers = headers == null ? new LinkedHashMap<>() : headers;
  }

  public void setPatch(List<JsonPatchOperation> patch) {
    this.patch = patch == null ? new ArrayList<>() : patch;
  }
}
