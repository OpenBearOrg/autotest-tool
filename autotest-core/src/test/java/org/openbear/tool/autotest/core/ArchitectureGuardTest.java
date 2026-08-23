package org.openbear.tool.autotest.core;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "org.openbear.tool.autotest.core")
class ArchitectureGuardTest {
  @ArchTest
  static final ArchRule coreDoesNotDependOnConcreteTransports =
      noClasses()
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "org.openbear.tool.autotest.http..",
              "org.openbear.tool.autotest.oracle..",
              "org.openbear.tool.autotest.mysql..",
              "org.openbear.tool.autotest.activemq..");
}
