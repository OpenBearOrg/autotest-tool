package org.openbear.tool.autotest.core.config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SecretRef {
  private String secret;

  public SecretRef() {}

  public SecretRef(String secret) {
    this.secret = secret;
  }
}
