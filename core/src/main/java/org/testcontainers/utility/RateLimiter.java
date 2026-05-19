package org.testcontainers.utility;

import java.time.Clock;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A rate limiter that enforces a minimum interval between invocations.
 * Subclasses implement {@link #getIntervalMs()} to define the spacing.
 * <p>
 * Permits are reserved atomically: concurrent callers each get a distinct
 * slot, so the rate cap holds under load. If the current thread is interrupted
 * while waiting for its slot, the runnable / callable is not executed and the
 * caller's outer retry loop sees the interrupt flag and bails.
 */
public abstract class RateLimiter {

    protected final Clock clock;

    private final AtomicLong nextAllowedAt = new AtomicLong(0);

    protected RateLimiter() {
        this(Clock.systemUTC());
    }

    protected RateLimiter(Clock clock) {
        this.clock = clock;
    }

    /**
     * Returns the minimum interval in milliseconds between invocations.
     */
    protected abstract long getIntervalMs();

    /**
     * Waits until permission is granted, then runs the runnable. If the
     * current thread is interrupted while waiting, the runnable is not
     * executed; the interrupt flag is restored so callers can react.
     */
    public void doWhenReady(Runnable runnable) {
        if (acquirePermission()) {
            runnable.run();
        }
    }

    /**
     * Waits until permission is granted, then calls the callable and returns
     * its result. If the current thread is interrupted while waiting, the
     * callable is not invoked and {@code null} is returned; the interrupt flag
     * is restored so callers can react.
     */
    public <T> T getWhenReady(Callable<T> callable) throws Exception {
        if (acquirePermission()) {
            return callable.call();
        }
        return null;
    }

    private boolean acquirePermission() {
        long intervalMs = getIntervalMs();
        long now = clock.millis();
        // Atomically reserve the next available slot. Concurrent callers each
        // advance nextAllowedAt by intervalMs, so they get distinct slots and
        // the rate cap is preserved.
        long mySlot = nextAllowedAt.updateAndGet(prev -> Math.max(now, prev) + intervalMs) - intervalMs;
        long waitMs = mySlot - now;
        if (waitMs > 0) {
            try {
                Thread.sleep(waitMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return true;
    }
}
