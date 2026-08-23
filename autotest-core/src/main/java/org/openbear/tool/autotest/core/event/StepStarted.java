package org.openbear.tool.autotest.core.event;

import org.openbear.tool.autotest.core.domain.ExecutionId;
import org.openbear.tool.autotest.core.domain.RunId;
import org.openbear.tool.autotest.core.domain.ScenarioId;
import org.openbear.tool.autotest.core.domain.StepId;

public record StepStarted(
    RunId runId,
    ExecutionId executionId,
    ScenarioId scenarioId,
    StepId stepId,
    String stepType,
    StepPhase phase)
    implements ExecutionEvent {}
