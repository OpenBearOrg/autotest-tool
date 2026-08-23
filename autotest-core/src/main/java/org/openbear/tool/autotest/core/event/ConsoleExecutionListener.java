package org.openbear.tool.autotest.core.event;

import java.io.PrintStream;
import java.util.Objects;

public final class ConsoleExecutionListener implements ExecutionListener {
  private final PrintStream output;

  public ConsoleExecutionListener() {
    this(System.out);
  }

  public ConsoleExecutionListener(PrintStream output) {
    this.output = Objects.requireNonNull(output, "output");
  }

  @Override
  public void onEvent(ExecutionEventEnvelope envelope) {
    switch (envelope.event()) {
      case RunStarted event ->
          output.printf(
              "Run started: %s project=%s environment=%s%n",
              event.runId().value(), event.project(), event.environment().value());
      case ScenarioStarted event ->
          output.printf("Scenario started: %s%n", event.scenarioId().value());
      case StepFinished event ->
          output.printf("  %-30s %-7s%n", event.stepId().value(), event.status());
      case ScenarioFinished event ->
          output.printf("Scenario finished: %s %s%n", event.scenarioId().value(), event.status());
      case RunFinished event ->
          output.printf("Run finished: %s scenarios=%d%n", event.status(), event.scenarioCount());
      case StepStarted ignored -> {}
    }
  }
}
