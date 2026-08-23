# Plugin authoring

AutoTest plugins depend on `autotest-spi`, not on the CLI or concrete built-in plugins.

## Minimal workflow

1. Add a dependency on `org.openbear.tool.autotest:autotest-spi`.
2. Implement `AutotestPlugin` and return a `PluginDescriptor`.
3. Implement a `StepTypeProvider` for each new DSL step.
4. Define the provider schema and configuration type.
5. Compile configuration into an immutable `ExecutableStep`.
6. Implement a `StepHandler` and return it from `PluginRuntime.stepHandlers()`.
7. Register the plugin in `META-INF/services/org.openbear.tool.autotest.spi.plugin.AutotestPlugin`.
8. Package the plugin as a JAR.
9. Load the JAR with the tool's plugin directory option when using the dynamic plugin loader.
10. Diagnose discovery with `autotest-tool list plugins` and `autotest-tool list step-types`.

Use `autotest-testkit` for compiler and plugin contract tests. Providers and handlers may be
called concurrently; keep them stateless or protect retained state. Do not put secrets in
generic lifecycle events or report evidence.

The SPI version is a runtime compatibility gate. Additive changes belong in minor releases;
breaking changes require a major release. See [SPI compatibility](../ai/spi-compatibility.md).
