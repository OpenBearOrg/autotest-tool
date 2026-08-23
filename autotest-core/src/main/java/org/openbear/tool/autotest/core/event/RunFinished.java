package org.openbear.tool.autotest.core.event;

import java.time.Duration;
import org.openbear.tool.autotest.core.domain.RunId;
import org.openbear.tool.autotest.core.model.ResultStatus;

public record RunFinished(
    RunId runId, ResultStatus status, Duration duration, int scenarioCount, String message)
    implements ExecutionEvent {}
