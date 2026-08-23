package org.openbear.tool.autotest.dsl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.openbear.tool.autotest.core.domain.CompiledStep;
import org.openbear.tool.autotest.core.plugin.PluginRegistry;
import org.openbear.tool.autotest.core.plugin.builtin.BuiltInStepPlugin;
import org.openbear.tool.autotest.dsl.compile.DynamicStepCompiler;
import org.openbear.tool.autotest.spi.service.ResourceAccess;
import org.openbear.tool.autotest.spi.step.StepCompileContext;

class DynamicStepCompilerTest {
  @Test
  void compilesBuiltInSetEnvelope() throws Exception {
    PluginRegistry plugins = new PluginRegistry().register(new BuiltInStepPlugin());
    JsonNode node =
        ObjectMappers.yaml().readTree("set:\n  id: set-values\n  values:\n    account: 42\n");
    CompiledStep step =
        new DynamicStepCompiler(plugins.stepTypes())
            .compile(node, new StepCompileContext("demo", "scenarios/demo.yaml", resources()));
    assertEquals("set-values", step.id().value());
    assertEquals("set", step.type());
    assertEquals(42, ((Map<?, ?>) step.configuration().get("values")).get("account"));
  }

  @Test
  void rejectsUnknownAndMultipleStepKeys() throws Exception {
    PluginRegistry plugins = new PluginRegistry().register(new BuiltInStepPlugin());
    DynamicStepCompiler compiler = new DynamicStepCompiler(plugins.stepTypes());
    assertThrows(
        IllegalArgumentException.class,
        () -> compiler.compile(ObjectMappers.yaml().readTree("custom: {}\n"), context()));
    assertThrows(
        IllegalArgumentException.class,
        () -> compiler.compile(ObjectMappers.yaml().readTree("set: {}\nassert: {}\n"), context()));
  }

  private static StepCompileContext context() {
    return new StepCompileContext("demo", "scenarios/demo.yaml", resources());
  }

  private static ResourceAccess resources() {
    return new ResourceAccess() {
      @Override
      public String readText(String workspaceRelativePath) {
        return "";
      }

      @Override
      public byte[] readBytes(String workspaceRelativePath) {
        return new byte[0];
      }
    };
  }
}
