package org.openbear.tool.autotest.core.event;

import org.openbear.tool.autotest.core.domain.ExecutionId;
import org.openbear.tool.autotest.core.domain.RunId;
import org.openbear.tool.autotest.core.domain.ScenarioId;

public record ScenarioStarted(
    RunId runId, ExecutionId executionId, ScenarioId scenarioId, String name)
    implements ExecutionEvent {}
