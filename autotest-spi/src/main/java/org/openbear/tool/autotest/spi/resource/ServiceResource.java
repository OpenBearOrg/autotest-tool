package org.openbear.tool.autotest.spi.resource;

import java.time.Duration;
import java.util.Map;

/** Immutable HTTP service configuration exposed to public transport plugins. */
public record ServiceResource(
    String name,
    String baseUrl,
    Map<String, String> defaultHeaders,
    Duration connectTimeout,
    Duration requestTimeout,
    String healthPath,
    int safeRetryAttempts) {}
