package org.openbear.tool.autotest.core.event;

import java.time.Duration;
import org.openbear.tool.autotest.core.domain.ExecutionId;
import org.openbear.tool.autotest.core.domain.RunId;
import org.openbear.tool.autotest.core.domain.ScenarioId;
import org.openbear.tool.autotest.core.domain.StepId;
import org.openbear.tool.autotest.core.model.ResultStatus;

public record StepFinished(
    RunId runId,
    ExecutionId executionId,
    ScenarioId scenarioId,
    StepId stepId,
    String stepType,
    StepPhase phase,
    ResultStatus status,
    Duration duration,
    String message)
    implements ExecutionEvent {}
