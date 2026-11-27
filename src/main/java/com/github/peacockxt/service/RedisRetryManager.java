package com.github.peacockxt.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Slf4j
public class RedisRetryManager {

    private static final int MAX_QUEUE_SIZE = 5_000;
    private static final int MAX_RETRIES = 3;
    private static final int BATCH_SIZE = 100;

    private final BlockingQueue<RetryTask> retryQueue = new LinkedBlockingQueue<>(MAX_QUEUE_SIZE);
    private final AtomicLong threadCounter = new AtomicLong(0);

    public void executeAsync(Runnable command) {
        Objects.requireNonNull(command, "command must not be null");
        Thread.ofVirtual().name("redis-backfill-", threadCounter.getAndIncrement()).start(() -> {
            try {
                command.run();
            } catch (Exception e) {
                log.warn("Async Redis task failed, enqueuing for retry: {}", e.getMessage());
                enqueue(new RetryTask(command));
            }
        });
    }

    public void execute(Runnable command) {
        Objects.requireNonNull(command, "command must not be null");
        enqueue(new RetryTask(command));
    }

    @Scheduled(fixedDelay = 5_000)
    public void process() {
        for (int i = 0; i < BATCH_SIZE; i++) {
            RetryTask task = retryQueue.poll();
            if (task == null) {
                break;
            }
            runWithRetry(task);
        }
    }

    private void runWithRetry(RetryTask task) {
        try {
            task.command.run();
            log.debug("Redis retry succeeded!");
        } catch (Exception e) {
            task.retryCount++;
            if (task.retryCount < MAX_RETRIES) {
                enqueue(task);
            } else {
                log.error("Redis retry failed after {} attempts. Dropping.", MAX_RETRIES, e);
            }
        }
    }

    private void enqueue(RetryTask task) {
        if (!retryQueue.offer(task)) {
            log.error("Redis retry queue full ({}). Dropping task to protect heap.", MAX_QUEUE_SIZE);
        }
    }

    private static final class RetryTask {
        final Runnable command;
        int retryCount = 0;

        RetryTask(Runnable command) {
            this.command = command;
        }
    }
}