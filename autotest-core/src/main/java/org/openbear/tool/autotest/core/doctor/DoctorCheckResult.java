package org.openbear.tool.autotest.core.doctor;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DoctorCheckResult {
  private String pluginId;
  private String name;
  private boolean success;
  private long durationMs;
  private String detail;

  public DoctorCheckResult() {}

  public DoctorCheckResult(
      String pluginId, String name, boolean success, long durationMs, String detail) {
    this.pluginId = pluginId;
    this.name = name;
    this.success = success;
    this.durationMs = durationMs;
    this.detail = detail;
  }

  public DoctorCheckResult(String name, boolean success, long durationMs, String detail) {
    this(null, name, success, durationMs, detail);
  }
}
