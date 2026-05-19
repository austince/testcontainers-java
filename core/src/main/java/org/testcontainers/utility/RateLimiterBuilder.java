package org.testcontainers.utility;

import java.util.concurrent.TimeUnit;

/**
 * Builder for creating {@link RateLimiter} instances.
 */
public class RateLimiterBuilder {

    private long intervalMs;

    private boolean strategySet;

    private RateLimiterBuilder() {}

    public static RateLimiterBuilder newBuilder() {
        return new RateLimiterBuilder();
    }

    /**
     * Sets the rate as {@code rate} invocations per {@code perTimeUnit}.
     * Uses integer division: {@code intervalMs = perTimeUnit.toMillis(1) / rate}.
     *
     * @param rate the number of invocations allowed
     * @param perTimeUnit the time unit for the rate
     * @return this builder
     */
    public RateLimiterBuilder withRate(int rate, TimeUnit perTimeUnit) {
        this.intervalMs = perTimeUnit.toMillis(1) / rate;
        return this;
    }

    /**
     * Enables constant throughput mode (sleep-based pacing).
     *
     * @return this builder
     */
    public RateLimiterBuilder withConstantThroughput() {
        this.strategySet = true;
        return this;
    }

    /**
     * Builds the {@link RateLimiter}.
     *
     * @return a new RateLimiter instance
     * @throws IllegalStateException if no strategy was set
     */
    public RateLimiter build() {
        if (!strategySet) {
            throw new IllegalStateException("A rate limiter strategy must be set (e.g. withConstantThroughput())");
        }
        final long capturedIntervalMs = this.intervalMs;
        return new RateLimiter() {
            @Override
            protected long getIntervalMs() {
                return capturedIntervalMs;
            }
        };
    }
}
