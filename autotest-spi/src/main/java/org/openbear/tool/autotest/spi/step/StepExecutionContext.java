package org.openbear.tool.autotest.spi.step;

public record StepExecutionContext(
    String runId, String executionId, String scenarioId, StepRuntimeServices services) {}
