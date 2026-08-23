package org.openbear.tool.autotest.spi.doctor;

import java.util.Map;

public record DoctorCheckResult(
    String id, DoctorStatus status, String message, Map<String, Object> details) {
  public DoctorCheckResult {
    details = details == null ? Map.of() : Map.copyOf(details);
  }
}
