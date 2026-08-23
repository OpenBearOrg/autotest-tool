package org.openbear.tool.autotest.dsl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SchemaValidator {
  private final ObjectMapper yamlMapper;
  private final JsonSchemaFactory factory =
      JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);

  public SchemaValidator(ObjectMapper yamlMapper) {
    this.yamlMapper = yamlMapper;
  }

  public JsonNode validate(String yaml, String schemaResource) {
    try (InputStream in =
        SchemaValidator.class.getClassLoader().getResourceAsStream(schemaResource)) {
      if (in == null) throw new IllegalStateException("Missing embedded schema: " + schemaResource);
      JsonSchema schema = factory.getSchema(in);
      JsonNode node = yamlMapper.readTree(yaml);
      Set<ValidationMessage> messages = schema.validate(node);
      if (!messages.isEmpty()) {
        List<String> errors = new ArrayList<>();
        messages.stream().map(ValidationMessage::getMessage).sorted().forEach(errors::add);
        throw new ValidationException("Schema validation failed", errors);
      }
      return node;
    } catch (ValidationException e) {
      throw e;
    } catch (Exception e) {
      throw new ValidationException("Unable to validate YAML", List.of(e.getMessage()));
    }
  }

  public JsonNode validate(JsonNode node, String schemaResource) {
    try (InputStream in =
        SchemaValidator.class.getClassLoader().getResourceAsStream(schemaResource)) {
      if (in == null) throw new IllegalStateException("Missing embedded schema: " + schemaResource);
      JsonSchema schema = factory.getSchema(in);
      Set<ValidationMessage> messages = schema.validate(node);
      if (!messages.isEmpty()) {
        List<String> errors = new ArrayList<>();
        messages.stream().map(ValidationMessage::getMessage).sorted().forEach(errors::add);
        throw new ValidationException("Schema validation failed", errors);
      }
      return node;
    } catch (ValidationException e) {
      throw e;
    } catch (Exception e) {
      throw new ValidationException(
          "Unable to validate step configuration", List.of(e.getMessage()));
    }
  }
}
