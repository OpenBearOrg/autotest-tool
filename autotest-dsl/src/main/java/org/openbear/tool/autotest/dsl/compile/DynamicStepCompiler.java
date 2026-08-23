package org.openbear.tool.autotest.dsl.compile;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import org.openbear.tool.autotest.core.domain.CompiledStep;
import org.openbear.tool.autotest.core.domain.StepId;
import org.openbear.tool.autotest.core.plugin.StepTypeRegistry;
import org.openbear.tool.autotest.dsl.ObjectMappers;
import org.openbear.tool.autotest.dsl.SchemaValidator;
import org.openbear.tool.autotest.spi.step.ExecutableStep;
import org.openbear.tool.autotest.spi.step.StepCompileContext;
import org.openbear.tool.autotest.spi.step.StepConfiguration;
import org.openbear.tool.autotest.spi.step.StepIdentity;
import org.openbear.tool.autotest.spi.step.StepTypeProvider;

/** Compiles a one-key DSL step envelope through a plugin-contributed provider. */
public final class DynamicStepCompiler {
  private final StepTypeRegistry registry;
  private final ObjectMapper mapper;
  private final SchemaValidator schemas;

  public DynamicStepCompiler(StepTypeRegistry registry) {
    this(registry, ObjectMappers.yaml());
  }

  public DynamicStepCompiler(StepTypeRegistry registry, ObjectMapper mapper) {
    this.registry = Objects.requireNonNull(registry, "registry");
    this.mapper = Objects.requireNonNull(mapper, "mapper");
    this.schemas = new SchemaValidator(mapper);
  }

  public CompiledStep compile(JsonNode envelope, StepCompileContext context) {
    Objects.requireNonNull(envelope, "envelope");
    Objects.requireNonNull(context, "context");
    if (!envelope.isObject() || envelope.size() != 1)
      throw new IllegalArgumentException("A step must contain exactly one step type");
    Iterator<Map.Entry<String, JsonNode>> fields = envelope.fields();
    Map.Entry<String, JsonNode> entry = fields.next();
    String type = entry.getKey();
    StepTypeProvider<?, ?> provider = registry.require(type);
    JsonNode body = entry.getValue();
    if (!body.isObject())
      throw new IllegalArgumentException("Step '" + type + "' must be an object");
    if (provider.schemaResource() != null && !provider.schemaResource().isBlank())
      schemas.validate(body, provider.schemaResource());
    StepConfiguration configuration = configuration(provider, body);
    ExecutableStep executable = compile(provider, configuration, context);
    StepIdentity identity = executable.identity();
    if (!type.equals(executable.type()))
      throw new IllegalStateException(
          "Provider " + type + " compiled an executable with type " + executable.type());
    Map<String, Object> values = mapper.convertValue(body, new TypeReference<>() {});
    return new CompiledStep(
        new StepId(identity.id()),
        identity.name(),
        identity.description(),
        identity.continueOnFailure(),
        type,
        values,
        executable);
  }

  @SuppressWarnings("unchecked")
  private static StepConfiguration configuration(StepTypeProvider<?, ?> provider, JsonNode body) {
    return (StepConfiguration)
        ObjectMappers.json().convertValue(body, provider.configurationType());
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static ExecutableStep compile(
      StepTypeProvider provider, StepConfiguration configuration, StepCompileContext context) {
    return (ExecutableStep) provider.compile(configuration, context);
  }
}
