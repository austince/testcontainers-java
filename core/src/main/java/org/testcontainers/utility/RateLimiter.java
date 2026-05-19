package org.testcontainers.utility;

import java.time.Clock;
import java.util.concurrent.Callable;

/**
 * A rate limiter that enforces a minimum interval between invocations.
 * Subclasses implement {@link #getWaitBeforeNextInvocation()} to define the delay.
 */
public abstract class RateLimiter {

    protected final Clock clock;

    protected volatile long lastInvocation;

    protected RateLimiter() {
        this(Clock.systemUTC());
    }

    protected RateLimiter(Clock clock) {
        this.clock = clock;
    }

    /**
     * Returns the number of milliseconds to wait before the next invocation is allowed.
     *
     * @return ms to wait, or 0 if ready immediately
     */
    protected abstract long getWaitBeforeNextInvocation();

    /**
     * Sleeps if needed, then executes the given runnable.
     *
     * @param runnable the action to run
     */
    public void doWhenReady(Runnable runnable) {
        sleepIfNeeded();
        try {
            runnable.run();
        } finally {
            lastInvocation = clock.millis();
        }
    }

    /**
     * Sleeps if needed, then calls the given callable and returns its result.
     *
     * @param callable the callable to invoke
     * @param <T> the return type
     * @return the result of the callable
     * @throws Exception if the callable throws
     */
    public <T> T getWhenReady(Callable<T> callable) throws Exception {
        sleepIfNeeded();
        try {
            return callable.call();
        } finally {
            lastInvocation = clock.millis();
        }
    }

    private void sleepIfNeeded() {
        long waitMs = getWaitBeforeNextInvocation();
        if (waitMs > 0) {
            try {
                Thread.sleep(waitMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
