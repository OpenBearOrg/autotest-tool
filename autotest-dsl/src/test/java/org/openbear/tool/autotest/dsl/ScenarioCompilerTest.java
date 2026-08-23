package org.openbear.tool.autotest.dsl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openbear.tool.autotest.core.model.ScenarioPlan;
import org.openbear.tool.autotest.core.util.Workspace;

class ScenarioCompilerTest {
  @TempDir Path temp;

  @Test
  void compilesValidScenarioAndChecksResources() throws Exception {
    Files.createDirectories(temp.resolve("scenarios"));
    Files.createDirectories(temp.resolve("payloads"));
    Files.writeString(temp.resolve("payloads/request.json"), "{\"id\":\"x\"}");
    Files.writeString(
        temp.resolve("scenarios/test.yaml"),
        """
				dslVersion: "1.0"
				id: TEST-001
				name: Test
				variables:
				  resourceId: 1
				steps:
				  - http:
				      id: create
				      service: api
				      request:
				        method: POST
				        path: /resources
				        bodyFile: payloads/request.json
				      expect:
				        status: 201
				""");
    ScenarioPlan plan =
        new ScenarioCompiler(new Workspace(temp)).compile(temp.resolve("scenarios/test.yaml"));
    assertEquals("TEST-001", plan.scenario().getId());
    assertTrue(plan.resourceChecksums().containsKey("payloads/request.json"));
  }

  @Test
  void rejectsUndeclaredVariableBeforeExecution() throws Exception {
    Files.createDirectories(temp.resolve("scenarios"));
    Files.writeString(
        temp.resolve("scenarios/bad.yaml"),
        """
				dslVersion: "1.0"
				id: TEST-002
				name: Bad
				steps:
				  - set:
				      id: x
				      values:
				        value: "${missing}"
				""");
    assertThrows(
        ValidationException.class,
        () ->
            new ScenarioCompiler(new Workspace(temp)).compile(temp.resolve("scenarios/bad.yaml")));
  }

  @Test
  void acceptsSupportedRuntimeExpressionsAndRejectsUnknownOnes() throws Exception {
    Files.createDirectories(temp.resolve("scenarios"));
    Files.writeString(
        temp.resolve("scenarios/random.yaml"),
        """
				dslVersion: "1.0"
				id: TEST-RANDOM
				name: Random values
				variables:
				  invalid: "${random:unknown}"
				steps:
				  - set:
				      id: random
				      values:
				        id: "${random:uuid}"
				        sequence: "${random:int}"
				""");
    assertThrows(
        ValidationException.class,
        () ->
            new ScenarioCompiler(new Workspace(temp))
                .compile(temp.resolve("scenarios/random.yaml")));

    Files.writeString(
        temp.resolve("scenarios/valid-random.yaml"),
        """
				dslVersion: "1.0"
				id: TEST-RANDOM-VALID
				name: Valid random value
				steps:
				  - set:
				      id: random
				      values:
				        id: "${random:uuid}"
				""");
    assertDoesNotThrow(
        () ->
            new ScenarioCompiler(new Workspace(temp))
                .compile(temp.resolve("scenarios/valid-random.yaml")));
  }

  @Test
  void allowsSameStepCaptureReferencesInAwaitSqlExpect() throws Exception {
    Files.createDirectories(temp.resolve("scenarios"));
    Files.createDirectories(temp.resolve("sql/resource"));
    Files.writeString(temp.resolve("sql/resource/test.sql"), "select 1 from dual");
    Files.writeString(
        temp.resolve("scenarios/await-same-step.yaml"),
        """
				dslVersion: "1.0"
				id: TEST-003
				name: Await same-step capture
				variables:
                  resourceType: SAMPLE_RESOURCE
				steps:
				  - awaitSql:
				      id: verify
				      connection: db
				      queryFile: sql/resource/test.sql
				      capture:
				        resourceId:
				          jsonPath: "$.rows[0].ID"
				      expect:
				        rowCount: 1
				        values:
				          "$.rows[0].ID": "${resourceId}"
				""");
    assertDoesNotThrow(
        () ->
            new ScenarioCompiler(new Workspace(temp))
                .compile(temp.resolve("scenarios/await-same-step.yaml")));
  }
}
