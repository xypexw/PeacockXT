package com.github.peacockxt.infrastructure.cache;

import com.github.peacockxt.service.RedisRetryManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

@Component
@Slf4j
@RequiredArgsConstructor
public class RedisOperationGuard {

    private static final int FAILURE_THRESHOLD = 5;
    private static final long OPEN_WINDOW_MS = 15_000L;

    private final RedisRetryManager redisRetryManager;
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicLong circuitOpenUntil = new AtomicLong(0L);

    private boolean isCircuitOpen() {
        return System.currentTimeMillis() < circuitOpenUntil.get();
    }

    private void recordSuccess() {
        if (consecutiveFailures.get() != 0) {
            consecutiveFailures.set(0);
        }
        if (circuitOpenUntil.get() != 0L) {
            circuitOpenUntil.set(0L);
        }
    }

    private void recordFailure(String actionName, Exception e) {
        int failures = consecutiveFailures.incrementAndGet();
        if (failures >= FAILURE_THRESHOLD) {
            long until = System.currentTimeMillis() + OPEN_WINDOW_MS;
            circuitOpenUntil.set(until);
            log.error("Redis circuit OPENED for {} ms after {} consecutive failures (last action [{}]): {}",
                    OPEN_WINDOW_MS, failures, actionName, e.getMessage());
        }
    }

    /**
     * Guard for READ operations (Get, Members, HasKey, etc.)
     */
    public <T> T safeRead(Supplier<T> action, T fallbackValue, String actionName) {
        if (isCircuitOpen()) {
            log.debug("Redis circuit OPEN, fast-failing READ [{}]", actionName);
            return fallbackValue;
        }
        try {
            T result = action.get();
            recordSuccess();
            return result;
        } catch (Exception e) {
            recordFailure(actionName, e);
            log.error("Redis READ error during [{}]: {}", actionName, e.getMessage());
            return fallbackValue;
        }
    }

    /**
     * Guard for cache-population / update WRITE operations.
     * Failures are NOT queued to RetryManager to prevent stale cache overwrite or resurrection.
     */
    public void safeWrite(Runnable action, String actionName) {
        if (isCircuitOpen()) {
            log.debug("Redis circuit OPEN, skipping cache WRITE [{}]", actionName);
            return;
        }
        try {
            action.run();
            recordSuccess();
            log.debug("Redis WRITE success: [{}]", actionName);
        } catch (Exception e) {
            recordFailure(actionName, e);
            log.warn("Redis WRITE error during [{}]. Skipping stale retry: {}", actionName, e.getMessage());
        }
    }

    /**
     * Guard for cache INVALIDATION / DELETE operations (DEL, SREM, ZREM, HDEL).
     * Delegated to RetryManager on failure or open circuit so stale keys are evicted upon recovery.
     */
    public void safeInvalidate(Runnable action, String actionName) {
        if (isCircuitOpen()) {
            log.warn("Redis circuit OPEN during invalidation [{}]. Delegating to Retry Manager.", actionName);
            redisRetryManager.execute(action);
            return;
        }
        try {
            action.run();
            recordSuccess();
            log.debug("Redis INVALIDATE success: [{}]", actionName);
        } catch (Exception e) {
            recordFailure(actionName, e);
            log.warn("Redis INVALIDATE error during [{}]. Delegating to Retry Manager: {}", actionName, e.getMessage());
            redisRetryManager.execute(action);
        }
    }
}
