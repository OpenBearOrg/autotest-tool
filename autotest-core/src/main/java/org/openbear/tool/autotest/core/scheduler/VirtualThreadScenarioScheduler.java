package org.openbear.tool.autotest.core.scheduler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import org.openbear.tool.autotest.core.domain.RunId;
import org.openbear.tool.autotest.core.model.ScenarioResult;

public final class VirtualThreadScenarioScheduler implements ScenarioScheduler {
  private final ResourceLockManager resourceLocks;

  public VirtualThreadScenarioScheduler() {
    this(new ResourceLockManager());
  }

  public VirtualThreadScenarioScheduler(ResourceLockManager resourceLocks) {
    this.resourceLocks = resourceLocks;
  }

  @Override
  public <T> List<ScenarioResult> execute(
      RunId runId,
      List<T> scenarios,
      ScenarioExecutionFunction<T> execution,
      SchedulingOptions options) {
    return execute(runId, scenarios, execution, options, ScenarioPolicyProvider.defaults());
  }

  public <T> List<ScenarioResult> execute(
      RunId runId,
      List<T> scenarios,
      ScenarioExecutionFunction<T> execution,
      SchedulingOptions options,
      ScenarioPolicyProvider<T> policy) {
    if (scenarios == null || scenarios.isEmpty()) return List.of();
    if (options.parallelism() == 1) return executeSequential(scenarios, execution, options);

    List<ScenarioResult> ordered = new ArrayList<>();
    Map<Integer, ScenarioResult> completed = new HashMap<>();
    try (ExecutorService executor =
        java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
      CompletionService<IndexedResult> completions = new ExecutorCompletionService<>(executor);
      Semaphore permits = new Semaphore(options.parallelism());
      Map<Future<IndexedResult>, Integer> active = new HashMap<>();
      boolean failed = false;
      int next = 0;
      while (next < scenarios.size() || !active.isEmpty()) {
        if (failed) {
          collectAll(active, completions, completed);
          break;
        }
        Future<IndexedResult> availableFuture;
        while ((availableFuture = completions.poll()) != null) {
          IndexedResult available = get(availableFuture);
          active.remove(findFuture(active, available.index()));
          completed.put(available.index(), available.result());
          if (options.failFast() && available.result().getStatus().isFailure()) failed = true;
        }
        if (failed) {
          collectAll(active, completions, completed);
          break;
        }
        if (next < scenarios.size() && policy.parallelSafe(scenarios.get(next))) {
          while (next < scenarios.size()
              && policy.parallelSafe(scenarios.get(next))
              && active.size() < options.parallelism()) {
            int index = next++;
            active.put(
                completions.submit(
                    () -> {
                      permits.acquire();
                      try (ResourceLockManager.LockHandle ignored =
                          resourceLocks.acquire(policy.claims(scenarios.get(index)))) {
                        return new IndexedResult(index, execution.execute(scenarios.get(index)));
                      } finally {
                        permits.release();
                      }
                    }),
                index);
          }
          if (active.isEmpty()) continue;
          IndexedResult result = take(completions);
          active.remove(findFuture(active, result.index()));
          completed.put(result.index(), result.result());
          failed = options.failFast() && result.result().getStatus().isFailure();
          continue;
        }

        collectAll(active, completions, completed);
        active.clear();
        if (options.failFast()
            && completed.values().stream().anyMatch(r -> r.getStatus().isFailure())) break;
        if (next >= scenarios.size()) break;
        ScenarioResult result = executeWithLocks(scenarios.get(next), execution, policy);
        completed.put(next++, result);
        if (options.failFast() && result.getStatus().isFailure()) break;
      }
    }
    for (int index = 0; index < scenarios.size(); index++) {
      ScenarioResult result = completed.get(index);
      if (result == null) break;
      ordered.add(result);
    }
    return ordered;
  }

  private <T> List<ScenarioResult> executeSequential(
      List<T> scenarios, ScenarioExecutionFunction<T> execution, SchedulingOptions options) {
    List<ScenarioResult> results = new ArrayList<>();
    for (T scenario : scenarios) {
      ScenarioResult result = execution.execute(scenario);
      results.add(result);
      if (options.failFast() && result.getStatus().isFailure()) break;
    }
    return results;
  }

  private <T> ScenarioResult executeWithLocks(
      T scenario, ScenarioExecutionFunction<T> execution, ScenarioPolicyProvider<T> policy) {
    try (ResourceLockManager.LockHandle ignored = resourceLocks.acquire(policy.claims(scenario))) {
      return execution.execute(scenario);
    }
  }

  private static <T> void collectAll(
      Map<Future<IndexedResult>, Integer> active,
      CompletionService<IndexedResult> completions,
      Map<Integer, ScenarioResult> completed) {
    while (!active.isEmpty()) {
      IndexedResult result = take(completions);
      active.remove(findFuture(active, result.index()));
      completed.put(result.index(), result.result());
    }
  }

  private static IndexedResult take(CompletionService<IndexedResult> completions) {
    try {
      return get(completions.take());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Scenario scheduler interrupted", e);
    }
  }

  private static IndexedResult get(Future<IndexedResult> future) {
    try {
      return future.get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Scenario scheduler interrupted", e);
    } catch (ExecutionException e) {
      throw new IllegalStateException("Scenario execution failed", e.getCause());
    }
  }

  private static Future<IndexedResult> findFuture(
      Map<Future<IndexedResult>, Integer> active, int index) {
    return active.entrySet().stream()
        .filter(entry -> entry.getValue() == index)
        .map(Map.Entry::getKey)
        .findFirst()
        .orElseThrow();
  }

  private record IndexedResult(int index, ScenarioResult result) {}
}
