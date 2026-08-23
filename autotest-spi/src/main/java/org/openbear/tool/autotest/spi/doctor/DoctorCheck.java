package org.openbear.tool.autotest.spi.doctor;

public interface DoctorCheck {
  String id();

  DoctorCheckResult run();
}
