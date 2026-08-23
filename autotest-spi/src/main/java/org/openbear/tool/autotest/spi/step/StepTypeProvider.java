package org.openbear.tool.autotest.spi.step;

/**
 * Defines a DSL step type and compiles configuration into an immutable executable step. Providers
 * may be reused concurrently and therefore must not retain compilation state.
 *
 * @param <C> serialized configuration type
 * @param <S> executable runtime type
 * @since 1.0
 */
public interface StepTypeProvider<C extends StepConfiguration, S extends ExecutableStep> {
  String type();

  Class<C> configurationType();

  String schemaResource();

  S compile(C configuration, StepCompileContext context);

  Class<S> executableType();
}
