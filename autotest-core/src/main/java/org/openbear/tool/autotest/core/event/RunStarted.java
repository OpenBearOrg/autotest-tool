package org.openbear.tool.autotest.core.event;

import org.openbear.tool.autotest.core.domain.EnvironmentId;
import org.openbear.tool.autotest.core.domain.RunId;

public record RunStarted(RunId runId, String project, EnvironmentId environment)
    implements ExecutionEvent {}
