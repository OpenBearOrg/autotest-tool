package org.openbear.tool.autotest.core.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class VariableResolverTest {
  @Test
  void resolvesEverySupportedRuntimeExpressionWithItsExpectedType() {
    Variables variables = variables();

    assertTrue(
        ((String) VariableResolver.resolve("${random:uuid}", variables))
            .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
    assertInstanceOf(Long.class, VariableResolver.resolve("${random:timestamp}", variables));
    assertTrue(
        ((String) VariableResolver.resolve("${random:isoTimestamp}", variables)).endsWith("Z"));
    assertEquals(21, ((String) VariableResolver.resolve("${random:nanoId}", variables)).length());
    assertTrue(
        ((String) VariableResolver.resolve("${random:alphaNumeric}", variables))
            .matches("[A-Za-z0-9]"));
    assertInstanceOf(Boolean.class, VariableResolver.resolve("${random:boolean}", variables));
    Object integer = VariableResolver.resolve("${random:int}", variables);
    assertInstanceOf(Integer.class, integer);
    assertTrue((Integer) integer >= 0 && (Integer) integer <= 1_000);
    assertTrue(
        ((String) VariableResolver.resolve("${random:hexColor}", variables))
            .matches("#[0-9a-f]{6}"));
    assertTrue(
        List.of("red", "fuchsia", "grey", "blue", "green")
            .contains(VariableResolver.resolve("${random:color}", variables)));
    assertTrue(
        List.of("SQL", "PCI", "JSON", "HTTP", "API")
            .contains(VariableResolver.resolve("${random:abbreviation}", variables)));
    assertTrue(
        List.of("protocol", "interface", "quick", "brown", "fox", "order", "customer")
            .contains(VariableResolver.resolve("${random:word}", variables)));
    assertEquals(
        3, ((String) VariableResolver.resolve("${random:words}", variables)).split(" ").length);
  }

  @Test
  void expandsEmbeddedExpressionsAndKeepsUserVariablesSeparate() {
    Variables variables = variables();

    assertEquals("customer-test", VariableResolver.resolve("${customerEnv}", variables));
    assertTrue(
        ((String) VariableResolver.resolve("request-${random:nanoId}", variables))
            .matches("request-[A-Za-z0-9_-]{21}"));
  }

  @Test
  void resolvesUserVariableAliasesRecursively() {
    Variables variables = variables();
    variables.put("EP_USER_SCOPES", "b2c_stc");
    variables.put("scope", "${EP_USER_SCOPES}");

    assertEquals("b2c_stc", VariableResolver.resolve("${scope}", variables));
    assertEquals(
        "/extcarts/b2c_stc/form", VariableResolver.resolve("/extcarts/${scope}/form", variables));
  }

  @Test
  void resolvesVariablesInsideExpectationMaps() {
    Variables variables = variables();
    variables.put("orderNumber", "210000067");

    Object resolved =
        VariableResolver.resolve(
            Map.of("$.rows[0].COM_PAYLOAD.externalId", Map.of("equals", "${orderNumber}")),
            variables);

    assertEquals(
        Map.of("$.rows[0].COM_PAYLOAD.externalId", Map.of("equals", "210000067")), resolved);
  }

  @Test
  void rejectsCircularUserVariableAliases() {
    Variables variables = variables();
    variables.put("first", "${second}");
    variables.put("second", "${first}");

    IllegalArgumentException failure =
        assertThrows(
            IllegalArgumentException.class, () -> VariableResolver.resolve("${first}", variables));

    assertTrue(failure.getMessage().contains("Circular variable reference"));
  }

  @Test
  void rejectsUnsupportedReservedExpressions() {
    assertThrows(
        IllegalArgumentException.class,
        () -> VariableResolver.resolve("${random:notSupported}", variables()));
  }

  private static Variables variables() {
    return new Variables() {
      private final Map<String, Object> values =
          new LinkedHashMap<>(Map.of("customerEnv", "customer-test"));

      @Override
      public Optional<Object> find(String name) {
        return Optional.ofNullable(values.get(name));
      }

      @Override
      public Object require(String name) {
        return find(name)
            .orElseThrow(() -> new IllegalArgumentException("Variable is not defined: " + name));
      }

      @Override
      public void put(String name, Object value) {
        values.put(name, value);
      }

      @Override
      public Map<String, Object> snapshot() {
        return Map.copyOf(values);
      }
    };
  }
}
