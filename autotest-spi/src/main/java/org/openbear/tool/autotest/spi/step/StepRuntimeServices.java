package org.openbear.tool.autotest.spi.step;

import java.time.Clock;
import org.openbear.tool.autotest.spi.resource.EnvironmentView;
import org.openbear.tool.autotest.spi.service.Assertions;
import org.openbear.tool.autotest.spi.service.JsonAccess;
import org.openbear.tool.autotest.spi.service.Polling;
import org.openbear.tool.autotest.spi.service.ResourceAccess;
import org.openbear.tool.autotest.spi.service.Secrets;
import org.openbear.tool.autotest.spi.service.Variables;

public record StepRuntimeServices(
    EnvironmentView environment,
    Variables variables,
    Assertions assertions,
    JsonAccess json,
    Polling polling,
    ResourceAccess resources,
    Secrets secrets,
    Clock clock) {}
