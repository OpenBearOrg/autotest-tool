package org.openbear.tool.autotest.core.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.openbear.tool.autotest.core.domain.RunId;
import org.openbear.tool.autotest.core.model.ResultStatus;
import org.openbear.tool.autotest.core.model.ScenarioResult;

class VirtualThreadScenarioSchedulerTest {
  @Test
  void boundsParallelismAndPreservesInputOrder() {
    VirtualThreadScenarioScheduler scheduler = new VirtualThreadScenarioScheduler();
    AtomicInteger running = new AtomicInteger();
    AtomicInteger maximum = new AtomicInteger();

    List<ScenarioResult> results =
        scheduler.execute(
            new RunId("run-1"),
            IntStream.range(0, 6).boxed().toList(),
            scenario -> {
              int current = running.incrementAndGet();
              maximum.accumulateAndGet(current, Math::max);
              try {
                Thread.sleep(25);
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
              } finally {
                running.decrementAndGet();
              }
              ScenarioResult result = new ScenarioResult();
              result.setId("scenario-" + scenario);
              result.setStatus(ResultStatus.PASS);
              return result;
            },
            new SchedulingOptions(2, false));

    assertTrue(maximum.get() <= 2);
    assertEquals(
        List.of("scenario-0", "scenario-1", "scenario-2", "scenario-3", "scenario-4", "scenario-5"),
        results.stream().map(ScenarioResult::getId).toList());
  }

  @Test
  void failFastDoesNotScheduleFutureScenarios() {
    VirtualThreadScenarioScheduler scheduler = new VirtualThreadScenarioScheduler();
    AtomicInteger calls = new AtomicInteger();

    List<ScenarioResult> results =
        scheduler.execute(
            new RunId("run-1"),
            IntStream.range(0, 8).boxed().toList(),
            scenario -> {
              calls.incrementAndGet();
              ScenarioResult result = new ScenarioResult();
              result.setId("scenario-" + scenario);
              result.setStatus(scenario == 0 ? ResultStatus.FAIL : ResultStatus.PASS);
              return result;
            },
            new SchedulingOptions(2, true));

    assertTrue(calls.get() <= 2);
    assertTrue(results.stream().anyMatch(result -> result.getStatus() == ResultStatus.FAIL));
    assertTrue(results.size() <= 2);
  }
}
