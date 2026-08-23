package org.openbear.tool.autotest.core.engine;

import java.util.List;
import org.openbear.tool.autotest.core.model.PollObservation;

public record PollOutcome<T>(
    boolean matched,
    T lastValue,
    List<PollObservation> observations,
    long elapsedMs,
    String timeoutMessage) {}
