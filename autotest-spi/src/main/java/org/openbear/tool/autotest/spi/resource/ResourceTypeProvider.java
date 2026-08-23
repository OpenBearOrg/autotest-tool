package org.openbear.tool.autotest.spi.resource;

/** Compiles an environment resource contributed by a plugin. @since 1.0 */
public interface ResourceTypeProvider<C, R extends PluginResource> {
  String type();

  Class<C> configurationType();

  String schemaResource();

  R compile(String resourceName, C configuration, ResourceCompileContext context);

  Class<R> resourceType();
}
