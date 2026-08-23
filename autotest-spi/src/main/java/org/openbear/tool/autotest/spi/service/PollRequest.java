package org.openbear.tool.autotest.spi.service;

import java.time.Duration;

public record PollRequest(Duration timeout, Duration interval) {}
