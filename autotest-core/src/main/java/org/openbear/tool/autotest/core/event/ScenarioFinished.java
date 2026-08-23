package org.openbear.tool.autotest.core.event;

import java.time.Duration;
import org.openbear.tool.autotest.core.domain.ExecutionId;
import org.openbear.tool.autotest.core.domain.RunId;
import org.openbear.tool.autotest.core.domain.ScenarioId;
import org.openbear.tool.autotest.core.model.ResultStatus;

public record ScenarioFinished(
    RunId runId,
    ExecutionId executionId,
    ScenarioId scenarioId,
    ResultStatus status,
    Duration duration,
    String message)
    implements ExecutionEvent {}
