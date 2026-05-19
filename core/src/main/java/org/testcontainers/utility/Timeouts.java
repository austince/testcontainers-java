package org.testcontainers.utility;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Utility for executing operations with a timeout using a per-call daemon executor.
 */
public final class Timeouts {

    private Timeouts() {}

    /**
     * Executes the callable with the given timeout, returning the result.
     *
     * @param timeout the timeout value (must be positive)
     * @param unit the time unit of the timeout
     * @param callable the operation to execute
     * @param <T> the return type
     * @return the result of the callable
     * @throws TimeoutException if the operation times out or is interrupted
     */
    public static <T> T getWithTimeout(int timeout, TimeUnit unit, Callable<T> callable) {
        if (timeout <= 0) {
            throw new TimeoutException("Timeout must be positive, was: " + timeout);
        }
        ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "testcontainers-timeout");
            t.setDaemon(true);
            return t;
        });
        try {
            Future<T> future = executor.submit(callable);
            return future.get(timeout, unit);
        } catch (java.util.concurrent.TimeoutException e) {
            throw new TimeoutException("Timeout after " + timeout + " " + unit, e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new RuntimeException(cause);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TimeoutException("Interrupted", e);
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * Executes the runnable with the given timeout.
     *
     * @param timeout the timeout value (must be positive)
     * @param unit the time unit of the timeout
     * @param runnable the operation to execute
     * @throws TimeoutException if the operation times out or is interrupted
     */
    public static void doWithTimeout(int timeout, TimeUnit unit, Runnable runnable) {
        getWithTimeout(
            timeout,
            unit,
            () -> {
                runnable.run();
                return null;
            }
        );
    }
}
