package org.openbear.tool.autotest.spi.resource;

import java.time.Duration;

/** Immutable messaging connection configuration exposed to public transport plugins. */
public record MessagingResource(
    String name,
    String provider,
    String brokerUrl,
    String usernameSecret,
    String passwordSecret,
    Duration connectTimeout) {}
