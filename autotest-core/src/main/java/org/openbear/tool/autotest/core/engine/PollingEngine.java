package org.openbear.tool.autotest.core.engine;

import static org.awaitility.Awaitility.await;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.awaitility.core.ConditionTimeoutException;
import org.openbear.tool.autotest.core.model.PollObservation;

public class PollingEngine {
  private final Clock clock;

  public PollingEngine() {
    this(Clock.systemUTC());
  }

  public PollingEngine(Clock clock) {
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  private long elapsedMs(Instant start) {
    return Duration.between(start, clock.instant()).toMillis();
  }

  public <T> PollOutcome<T> poll(
      String alias,
      Duration timeout,
      Duration interval,
      Supplier<T> supplier,
      Predicate<T> predicate,
      Function<T, Object> observationMapper) {
    Instant start = clock.instant();
    AtomicReference<T> last = new AtomicReference<>();
    List<PollObservation> observations = new ArrayList<>();
    AtomicReference<Object> lastObservation = new AtomicReference<>();
    try {
      await(alias)
          .pollDelay(Duration.ZERO)
          .pollInterval(interval)
          .atMost(timeout)
          .until(
              () -> {
                T value = supplier.get();
                last.set(value);
                Object observation = observationMapper.apply(value);
                if (!Objects.equals(lastObservation.get(), observation)) {
                  observations.add(
                      new PollObservation(clock.instant(), elapsedMs(start), observation));
                  lastObservation.set(observation);
                }
                return predicate.test(value);
              });
      return new PollOutcome<>(true, last.get(), List.copyOf(observations), elapsedMs(start), null);
    } catch (ConditionTimeoutException e) {
      return new PollOutcome<>(
          false, last.get(), List.copyOf(observations), elapsedMs(start), e.getMessage());
    }
  }
}
