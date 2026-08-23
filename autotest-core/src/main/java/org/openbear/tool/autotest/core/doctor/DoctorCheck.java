package org.openbear.tool.autotest.core.doctor;

@FunctionalInterface
public interface DoctorCheck {
  DoctorCheckResult check();
}
