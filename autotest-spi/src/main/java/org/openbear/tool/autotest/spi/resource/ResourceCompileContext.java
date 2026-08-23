package org.openbear.tool.autotest.spi.resource;

import org.openbear.tool.autotest.spi.service.ResourceAccess;

public record ResourceCompileContext(String environmentName, ResourceAccess resources) {}
