package org.openbear.tool.autotest.spi.step;

import org.openbear.tool.autotest.spi.service.ResourceAccess;

public record StepCompileContext(
    String scenarioId, String workspaceRelativeScenarioPath, ResourceAccess resources) {}
