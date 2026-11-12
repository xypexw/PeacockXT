package com.github.peacockxt.infrastructure.cache;

import com.github.peacockxt.service.RedisRetryManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
@Slf4j
@RequiredArgsConstructor
public class RedisOperationGuard {

    private final RedisRetryManager redisRetryManager;

    /**
     * Guard for READ operations (Get, Members, HasKey, etc.)
     * @param action        The read operation to execute
     * @param fallbackValue The value to return if Redis fails
     * @param actionName    A descriptive name for logging purposes
     * @return The result of the action, or the fallback value if an exception occurs
     */
    public <T> T safeRead(Supplier<T> action, T fallbackValue, String actionName) {
        try {
            return action.get();
        } catch (Exception e) {
            log.error("Redis READ error during [{}]: {}", actionName, e.getMessage());
            // Return fallback value (e.g., null or empty list) if Redis goes down
            return fallbackValue;
        }
    }

    /**
     * Guard for WRITE/DELETE operations (Set, Add, Delete, etc.)
     * @param action     The write operation to execute
     * @param actionName A descriptive name for logging purposes
     */
    public void safeWrite(Runnable action, String actionName) {
        try {
            action.run();
            log.debug("Redis WRITE success: [{}]", actionName);
        } catch (Exception e) {
            log.warn("Redis WRITE error during [{}]. Delegating to Retry Manager: {}", actionName, e.getMessage());
            // Hand over to the retry manager when a write operation fails
            redisRetryManager.execute(action);
        }
    }
}