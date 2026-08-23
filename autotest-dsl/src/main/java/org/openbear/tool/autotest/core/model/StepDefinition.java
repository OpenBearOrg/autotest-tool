package org.openbear.tool.autotest.core.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.Setter;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonSubTypes({
  @JsonSubTypes.Type(value = HttpStepDefinition.class, name = "http"),
  @JsonSubTypes.Type(value = SqlStepDefinition.class, name = "sql"),
  @JsonSubTypes.Type(value = AwaitSqlStepDefinition.class, name = "awaitSql"),
  @JsonSubTypes.Type(value = AwaitMessageStepDefinition.class, name = "awaitMessage"),
  @JsonSubTypes.Type(value = SetStepDefinition.class, name = "set"),
  @JsonSubTypes.Type(value = AssertStepDefinition.class, name = "assert")
})
@Getter
@Setter
public abstract class StepDefinition {
  private String id;
  private String name;
  private String description;
  private boolean continueOnFailure = false;

  public abstract String type();

  public String displayName() {
    return name == null || name.isBlank() ? id : name;
  }
}
