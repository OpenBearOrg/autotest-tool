package org.openbear.tool.autotest.testkit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openbear.tool.autotest.core.plugin.PluginRegistry;
import org.openbear.tool.autotest.spi.plugin.PluginRuntimeContext;

class ScenarioTestKitTest {
  @Test
  void keepsCurrentWorkspaceOnlyBehaviorByDefault(@TempDir Path root) throws IOException {
    Files.writeString(
        root.resolve("autotest-tool.yaml"),
        """
				projectVersion: "1.0"
				name: Upgrade Regression

				defaults:
				  environment: local
				  polling:
				    timeout: 2m
				    interval: 3s

				reporting:
				  directory: reports
				  payloadMaxSize: 1MB
				  redactedFields:
				    - authorization
				    - password
				    - token
				    - client_secret
				    - secret
				    - card_number

				execution:
				  parallelism: 1
				  failFast: false
				""");

    ScenarioTestKit kit = new ScenarioTestKit(root);

    assertEquals(root, kit.workspace().root());
    assertEquals("1.0", kit.project().getProjectVersion());
    assertEquals(0, kit.plugins().capabilities().all(String.class).size());
  }

  @Test
  void acceptsAnInjectedPluginRegistry(@TempDir Path root) {
    PluginRegistry registry =
        TestPlugins.registry(
            TestPlugins.plugin(
                "sample",
                "Sample Plugin",
                "1.0.0",
                TestPlugins.capability("sample-capability", String.class, "sample-value")));

    ScenarioTestKit kit = new ScenarioTestKit(root, registry);

    try (registry) {
      registry.open(
          new PluginRuntimeContext(
              () -> "test",
              new org.openbear.tool.autotest.spi.service.ResourceAccess() {
                @Override
                public String readText(String path) {
                  throw new UnsupportedOperationException();
                }

                @Override
                public byte[] readBytes(String path) {
                  throw new UnsupportedOperationException();
                }
              },
              reference -> java.util.Optional.empty(),
              Clock.systemUTC()));
      assertSame(registry, kit.plugins());
      assertEquals("sample-value", kit.plugins().capabilities().all(String.class).getFirst());
    }
  }
}
