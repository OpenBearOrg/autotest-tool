package org.openbear.tool.autotest.core.scheduler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class ResourceLockManager {
  private final Map<ResourceLockKey, ReentrantReadWriteLock> locks = new HashMap<>();

  public LockHandle acquire(Collection<ResourceClaim> claims) {
    List<ResourceClaim> ordered =
        claims == null
            ? List.of()
            : claims.stream().sorted(Comparator.comparing(ResourceClaim::key)).toList();
    List<AcquiredLock> acquired = new ArrayList<>();
    try {
      for (ResourceClaim claim : ordered) {
        ReentrantReadWriteLock lock;
        synchronized (locks) {
          lock = locks.computeIfAbsent(claim.key(), ignored -> new ReentrantReadWriteLock());
        }
        if (claim.mode() == ResourceLockMode.SHARED) lock.readLock().lock();
        else lock.writeLock().lock();
        acquired.add(new AcquiredLock(lock, claim.mode()));
      }
      return new LockHandle(acquired);
    } catch (RuntimeException e) {
      new LockHandle(acquired).close();
      throw e;
    }
  }

  public static final class LockHandle implements AutoCloseable {
    private final List<AcquiredLock> acquired;
    private boolean closed;

    private LockHandle(List<AcquiredLock> acquired) {
      this.acquired = List.copyOf(acquired);
    }

    @Override
    public void close() {
      if (closed) return;
      closed = true;
      for (int i = acquired.size() - 1; i >= 0; i--) acquired.get(i).unlock();
    }
  }

  private record AcquiredLock(ReentrantReadWriteLock lock, ResourceLockMode mode) {
    private void unlock() {
      if (mode == ResourceLockMode.SHARED) lock.readLock().unlock();
      else lock.writeLock().unlock();
    }
  }
}
